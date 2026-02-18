import React from "react";

export type ChatMessage = {
	id: string;
	role: "ASSISTANT" | "USER";
	content: string;
};

export type AssistantState = "IDLE" | "THINKING" | "ANSWERING";

type ChatContextValue = {
	chatHistory: ChatMessage[];
	newChat: () => void;
	prompt: (promptText: string) => void;
	retry: () => void;
	assistantState: AssistantState;
	answerStream: string | null;
	lastUserMessageId: string | undefined;
	isError: boolean;
};

type ChatProviderProps = { children: React.ReactNode };

type StreamData = {
	text: string;
	conversationId: string;
};

const CHAT_URL = "/chat";

const ChatContext = React.createContext<ChatContextValue | undefined>(
	undefined
);

const getChatHistory = async (): Promise<ChatMessage[]> => {
	const conversationId = new URL(window.location.href).searchParams.get("c");
	if (!conversationId) return [];

	const res = await fetch(
		`${CHAT_URL}/messages?c=${encodeURIComponent(conversationId)}`
	);

	if (!res.ok) throw new Error("Failed to fetch chat history");

	const messages = await res.json();

	return messages.map((m: Partial<ChatMessage>) => ({
		id: crypto.randomUUID(),
		role: m.role!,
		content: m.content!,
	}));
};


export function ChatProvider({ children }: ChatProviderProps) {
	const [chatHistory, setChatHistory] = React.useState<ChatMessage[]>([]);
	const [answerStream, setAnswerStream] = React.useState<string | null>(null);
	const [isError, setIsError] = React.useState(false);

	const eventSourceRef = React.useRef<EventSource | null>(null);
	const pendingMessageRef = React.useRef<string | null>(null);

	const assistantState: AssistantState =
		answerStream === null ? "IDLE" :
			answerStream === "" ? "THINKING" :
				"ANSWERING";

	const lastUserMessageId: string | undefined = chatHistory
		.filter((m) => m.role === "USER")
		.at(-1)?.id;

	React.useEffect(() => {
		getChatHistory()
			.then((v) => {
				if (v.length === 0) {
					const url = new URL(window.location.href);
					url.searchParams.delete("c");
					window.history.replaceState(null, "", url);
				}
				setChatHistory(v);
			})
			.catch(() => setChatHistory([]));

		return () => cleanUpStream();
	}, []);

	const cleanUpStream = () => {
		if (eventSourceRef.current) {
			eventSourceRef.current.close();
			eventSourceRef.current = null;
		}
	};

	const addMessage = (role: "USER" | "ASSISTANT", content: string) => {
		const id = crypto.randomUUID();
		setChatHistory((prev) => [...prev, { id, role, content }]);
	};

	const streamResponse = (messageText: string) => {
		cleanUpStream();
		setIsError(false);
		setAnswerStream("");

		const url = new URL(window.location.href);
		const searchParams = new URLSearchParams();
		const currentConversationId = url.searchParams.get("c");

		if (currentConversationId) {
			searchParams.set("c", currentConversationId);
		}
		searchParams.set("message", messageText);

		const eventSource = new EventSource(`${CHAT_URL}?${searchParams.toString()}`);
		eventSourceRef.current = eventSource;

		let accumulatedResponse = "";
		let answerConversationId: string | null = null;
		let isStreamComplete = false;

		eventSource.onmessage = (e) => {
			if (eventSource !== eventSourceRef.current) {
				eventSource.close();
				return;
			}

			const data = JSON.parse(e.data) as StreamData;

			accumulatedResponse += data.text;
			setAnswerStream(accumulatedResponse);

			if (!answerConversationId && data.conversationId) {
				answerConversationId = data.conversationId;
				url.searchParams.set("c", answerConversationId);
				window.history.replaceState(null, "", url);
			}
		};

		eventSource.addEventListener("stop", () => {
			if (eventSource !== eventSourceRef.current) return;

			isStreamComplete = true;
			cleanUpStream();

			addMessage("ASSISTANT", accumulatedResponse);
			setAnswerStream(null);
		});

		eventSource.onerror = () => {
			if (eventSource !== eventSourceRef.current) return;

			cleanUpStream();

			if (!isStreamComplete) {
				setIsError(true);
				setAnswerStream(null);
			}
		};
	};

	const prompt = (promptText: string) => {
		pendingMessageRef.current = promptText;

		addMessage("USER", promptText);
		streamResponse(promptText);
	};

	const retry = () => {
		const messageToRetry = pendingMessageRef.current;
		if (!messageToRetry) return;

		streamResponse(messageToRetry);
	};

	const newChat = () => {
		cleanUpStream();
		setAnswerStream(null);
		setIsError(false);
		setChatHistory([]);
		pendingMessageRef.current = null;

		window.history.replaceState(null, "", "/");
		document.title = "Nibras";
	};

	return (
		<ChatContext.Provider
			value={{
				chatHistory,
				newChat,
				prompt,
				retry,
				assistantState,
				answerStream,
				lastUserMessageId,
				isError,
			}}
		>
			{children}
		</ChatContext.Provider>
	);
}

export const useChat = () => {
	const context = React.useContext(ChatContext);

	if (!context) throw new Error("useChat must be used within a ChatProvider");

	return context;
};
