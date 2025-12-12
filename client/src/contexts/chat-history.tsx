import React from "react";

export type ChatMessage = {
  id: string;
  role: "bot" | "user";
  content: string;
  status: 'pending' | 'streaming' | 'done';
}

type ChatHistoryContextValue = {
  chatHistory: ChatMessage[];
  streamBotAnswer: (prompt: string) => void;
  isBotBusy: boolean;
}

const ChatHistoryContext = React.createContext<ChatHistoryContextValue | undefined>(undefined);

type ChatHistoryProviderProps = {
  children: React.ReactNode;
}

export function ChatHistoryProvider({ children }: ChatHistoryProviderProps) {
  const [chatHistory, setChatHistory] = React.useState<ChatMessage[]>([]);

  const addUserMessage = (content: string) => {
    const id = crypto.randomUUID();
    setChatHistory((prev) => [...prev, { id, role: 'user', content, status: 'done' }]);
  }

  const isBotBusy = chatHistory.some(m => m.status === 'streaming' || m.status === 'pending');

  const streamBotAnswer = (prompt: string) => {
    addUserMessage(prompt);

    const id = crypto.randomUUID();
    setChatHistory(prev => [...prev, { id, role: 'bot', content: "", status: 'pending' }]);

    const eventSource = new EventSource(`/ai/generate?message=${encodeURIComponent(prompt)}`);

    eventSource.onmessage = (e) => {
      const token = JSON.parse(e.data) as { text: string };

      setChatHistory((prev) => prev.map(m => (
        m.id === id
          ? { ...m, content: m.content + token.text, status: 'streaming' }
          : m
      )))
    }

    eventSource.onerror = () => {
      setChatHistory(prev =>
        prev.map(m =>
          m.id === id ? { ...m, status: "done" } : m
        )
      );
      eventSource.close();
    };
  }

  return (
    <ChatHistoryContext.Provider value={{ chatHistory, streamBotAnswer, isBotBusy }}>
      {children}
    </ChatHistoryContext.Provider>
  )
}

export const useChatHistory = () => {
  const context = React.useContext(ChatHistoryContext);

  if (!context) {
    throw new Error("We are Charlie Kirk");
  }

  return context;
}
