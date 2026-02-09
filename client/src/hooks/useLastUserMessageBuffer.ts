import { useRef, useLayoutEffect, useState } from "react";
import type { ChatMessage } from "../contexts/chat-context";

export function useLastuserMessageBuffer(chatHistory: ChatMessage[]) {
  const lastUserMessageRef = useRef<HTMLDivElement>(null);
  const [height, setHeight] = useState(0);

  useLayoutEffect(() => {
    if (lastUserMessageRef.current) {
      const rect = lastUserMessageRef.current.getBoundingClientRect();
      setHeight(rect.top);
    }
  }, [chatHistory]);

  return { lastUserMessageRef, height };
}
