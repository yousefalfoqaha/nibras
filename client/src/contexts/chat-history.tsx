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


const fakeMessages: ChatMessage[] = [
  { id: crypto.randomUUID(), role: 'user', content: "Hi! Can you tell me the university opening hours?", status: 'done' },
  { id: crypto.randomUUID(), role: 'bot', content: "Hello! Sure, the university is open from 8:00 AM to 5:00 PM, Sunday to Thursday.", status: 'done' },
  { id: crypto.randomUUID(), role: 'user', content: "And what about the library?", status: 'done' },
  { id: crypto.randomUUID(), role: 'bot', content: "The library is open from 7:00 AM to 10:00 PM on weekdays, and 9:00 AM to 6:00 PM on weekends.", status: 'done' },
  { id: crypto.randomUUID(), role: 'user', content: "Can I book a study room online?", status: 'done' },
  // { id: crypto.randomUUID(), role: 'bot', content: "Yes! You can book study rooms through the university portal under 'Library Services'.", status: 'done' },
  // { id: crypto.randomUUID(), role: 'user', content: "Great, thanks!", status: 'done' },
  // { id: crypto.randomUUID(), role: 'bot', content: "You're welcome! Would you like to see some frequently asked questions as well?", status: 'done' },
  // { id: crypto.randomUUID(), role: 'user', content: "Yes, please.", status: 'done' },
  // { id: crypto.randomUUID(), role: 'bot', content: "Here are some common questions:\n1. How to register for courses?\n2. How to check exam results?\n3. How to apply for scholarships?", status: 'done' },
];

export function ChatHistoryProvider({ children }: ChatHistoryProviderProps) {
  const [chatHistory, setChatHistory] = React.useState<ChatMessage[]>(fakeMessages);

  const addUserMessage = (content: string) => {
    const id = crypto.randomUUID();
    setChatHistory([...chatHistory, { id, role: 'user', content, status: 'done' }]);
  }

  const isBotBusy = chatHistory.some(m => m.status === 'streaming' || m.status === 'pending');

  const streamBotAnswer = (prompt: string) => {
    addUserMessage(prompt);

    const id = crypto.randomUUID();
    setChatHistory(prev => [...prev, { id, role: 'bot', content: "", status: 'pending' }]);

    const eventSource = new EventSource(`/ai/generate?message=${encodeURIComponent(prompt)}`);

    eventSource.onmessage = (e) => {
      setChatHistory((prev) => prev.map(m => (
        m.id === id
          ? { ...m, content: m.content + e.data, status: 'streaming' }
          : m
      )))
    }

    setChatHistory((prev) => prev.map(m => (
      m.id === id
        ? { ...m, status: 'done' }
        : m
    )));

    eventSource.close();
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
