import React from "react";

export type ChatMessage = {
  id: string;
  role: "ASSISTANT" | "USER";
  content: string;
  status: "PENDING" | "STREAMING" | "DONE";
};

type ChatHistoryContextValue = {
  chatHistory: ChatMessage[];
  streamBotAnswer: (prompt: string) => void;
  isBotBusy: boolean;
};

const CHAT_URL = "/chat";

const ChatHistoryContext = React.createContext<ChatHistoryContextValue | undefined>(
  undefined
);

type ChatHistoryProviderProps = { children: React.ReactNode };

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

  return res.json();
};

export function ChatHistoryProvider({ children }: ChatHistoryProviderProps) {
  const [chatHistory, setChatHistory] = React.useState<ChatMessage[]>([]);

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
      { id, role: "USER", content, status: "DONE" },
    ]);
  };

  const isBotBusy = chatHistory.some(
    (m) => m.status === "STREAMING" || m.status === "PENDING"
  );

  const streamBotAnswer = (prompt: string) => {
    addUserMessage(prompt);

    const id = crypto.randomUUID();
    setChatHistory((prev) => [
      ...prev,
      { id, role: "ASSISTANT", content: "", status: "PENDING" },
    ]);

    const url = new URL(window.location.href);
    const searchParams = new URLSearchParams();
    const currentConversationId = url.searchParams.get("c");

    if (currentConversationId) {
      searchParams.set("c", currentConversationId);
    }
    searchParams.set("message", prompt);

    let answerConversationId: string | null = null;

    const eventSource = new EventSource(`${CHAT_URL}?${searchParams.toString()}`);

    eventSource.onmessage = (e) => {
      const data = JSON.parse(e.data) as { text: string; conversationId: string };

      setChatHistory((prev) =>
        prev.map((m) =>
          m.id === id
            ? { ...m, content: m.content + data.text, status: "STREAMING" }
            : m
        )
      );

      if (!answerConversationId) {
        answerConversationId = data.conversationId;
        url.searchParams.set("c", answerConversationId);
        window.history.replaceState(null, "", url);
      }
    };

    eventSource.onerror = () => {
      setChatHistory((prev) =>
        prev.map((m) => (m.id === id ? { ...m, status: "DONE" } : m))
      );
      eventSource.close();
    };
  };

  return (
    <ChatHistoryContext.Provider
      value={{ chatHistory, streamBotAnswer, isBotBusy }}
    >
      {children}
    </ChatHistoryContext.Provider>
  );
}

export const useChatHistory = () => {
  const context = React.useContext(ChatHistoryContext);
  if (!context) throw new Error("We are Charlie Kirk");
  return context;
};
