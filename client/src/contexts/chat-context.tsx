import React from "react";

export type ChatMessage = {
  id: string;
  role: "ASSISTANT" | "USER";
  content: string;
};

export type AssistantState = "IDLE" | "THINKING" | "ANSWERING";

type ChatContextValue = {
  chatHistory: ChatMessage[];
  prompt: (prompt: string) => void;
  assistantState: AssistantState;
  answerStream: string | null;
};

const CHAT_URL = "/chat";

const ChatContext = React.createContext<ChatContextValue | undefined>(
  undefined
);

type ChatProviderProps = { children: React.ReactNode };

const getChatHistory = async (): Promise<ChatMessage[]> => {
  const conversationId = new URL(window.location.href).searchParams.get("c");

  if (!conversationId) {
    return [];
  }

  const res = await fetch(
    `${CHAT_URL}/messages?c=${encodeURIComponent(conversationId)}`
  );

  if (!res.ok) {
    throw new Error("Failed to fetch chat history");
  }

  const messages = await res.json();

  return messages.map((m: Partial<ChatMessage>) => ({
    id: crypto.randomUUID(),
    role: m.role!,
    content: m.content!
  } as ChatMessage));
};

export function ChatProvider({ children }: ChatProviderProps) {
  const [chatHistory, setChatHistory] = React.useState<ChatMessage[]>([]);
  const [answerStream, setAnswerStream] = React.useState<string | null>(null);

  const assistantState: AssistantState =
    answerStream === null ? "IDLE" :
      answerStream === "" ? "THINKING" :
        "ANSWERING";

  React.useEffect(() => {
    getChatHistory()
      .then(v => {
        if (v.length === 0) {
          const url = new URL(window.location.href);
          url.searchParams.delete("c");
          window.history.replaceState(null, "", url);
        }
        setChatHistory(v);
      })
      .catch(() => setChatHistory([]));
  }, []);

  const addUserMessage = (content: string) => {
    const id = crypto.randomUUID();
    setChatHistory((prev) => [
      ...prev,
      { id, role: "USER", content },
    ]);
  };

  const addAssistantMessage = (content: string) => {
    const id = crypto.randomUUID();
    setChatHistory((prev) => [
      ...prev,
      { id, role: "ASSISTANT", content },
    ]);
  }

  const prompt = (promptText: string) => {
    addUserMessage(promptText);
    setAnswerStream("");

    const url = new URL(window.location.href);
    const searchParams = new URLSearchParams();
    const currentConversationId = url.searchParams.get("c");
    if (currentConversationId) {
      searchParams.set("c", currentConversationId);
    }
    searchParams.set("message", promptText);

    let accumulatedResponse = "";
    let answerConversationId: string | null = null;

    const eventSource = new EventSource(`${CHAT_URL}?${searchParams.toString()}`);

    eventSource.onmessage = (e) => {
      const data = JSON.parse(e.data) as { text: string; conversationId: string };

      accumulatedResponse += data.text;

      setAnswerStream((prev) => (prev || "") + data.text);

      if (!answerConversationId) {
        answerConversationId = data.conversationId;
        url.searchParams.set("c", answerConversationId);
        window.history.replaceState(null, "", url);
      }
    };

    eventSource.onerror = () => {
      eventSource.close();

      if (accumulatedResponse) {
        addAssistantMessage(accumulatedResponse);
      }

      setAnswerStream(null);
    };
  };

  return (
    <ChatContext.Provider
      value={{
        chatHistory,
        prompt,
        assistantState,
        answerStream
      }}
    >
      {children}
    </ChatContext.Provider>
  );
}

export const useChat = () => {
  const context = React.useContext(ChatContext);
  if (!context) throw new Error("We are Charlie Kirk");
  return context;
};
