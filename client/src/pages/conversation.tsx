import { Avatar, Image, Typography } from '@mantine/core';
import { UserInput } from '../components/user-input';
import styles from './conversation.module.css';
import { useChat, type ChatMessage } from '../contexts/chat-context';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import React from 'react';
import { UserRound } from 'lucide-react';
import nibrasIdle from '/nibras-idle.png';
import nibrasThinking from '/nibras-thinking.png';
import nibrasAnswering from '/nibras-answering.png';

const AVATAR_IMAGES = {
  IDLE: nibrasIdle,
  THINKING: nibrasThinking,
  ANSWERING: nibrasAnswering,
};

export function Conversation() {
  const { chatHistory, answerStream } = useChat();
  // console.log(answerStream)

  return (
    <main className={styles.conversation}>
      <AutoScroll>
        <div className={styles.messages}>
          {chatHistory.map((m) =>
            m.role === "USER" ? (
              <UserMessageBubble key={m.id} message={m} />
            ) : (
              <AssistantMessageMarkdown key={m.id} message={m} />
            )
          )}

          {answerStream && (
            <AssistantMessageMarkdown
              key="answer-stream"
              message={{ id: "streaming", role: "ASSISTANT", content: answerStream }}
            />
          )}

          <AssistantAvatar />
        </div>
      </AutoScroll>

      <ChatInterface />
    </main>
  );
}

type UserMessageBubbleProps = {
  message: ChatMessage;
};

function UserMessageBubble({ message }: UserMessageBubbleProps) {
  return (
    <div className={styles.userMessage}>
      <Avatar color="var(--mantine-color-primary-filled)" variant="transparent">
        <UserRound />
      </Avatar>
      {message.content}
    </div>
  );
}

type AssistantMessageMarkdownProps = {
  message: ChatMessage;
};

function AssistantMessageMarkdown({ message }: AssistantMessageMarkdownProps) {
  return (
    <Typography>
      <Markdown remarkPlugins={[remarkGfm]}>{message.content}</Markdown>
    </Typography>
  );
}

function AssistantAvatar() {
  const { assistantState } = useChat();

  return (
    <div
      className={styles.assistantAvatar}
      data-avatar-state={assistantState}
      role="img"
      aria-label={`Assistant ${assistantState}`}
    >
      <Image src={AVATAR_IMAGES[assistantState]} w={100} h="auto" />
    </div>
  );
}

function ChatInterface() {
  return (
    <section className={styles.chatInterface}>
      <UserInput />

      <p className={styles.disclaimer}>
        Nibras can make mistakes, check with an academic advisor.
      </p>
    </section>
  );
}

function AutoScroll({ children }: { children: React.ReactNode }) {
  const { chatHistory } = useChat();
  const bottomRef = React.useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = React.useState(true);
  const isScrollingRef = React.useRef(false);

  React.useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!isScrollingRef.current) setAutoScroll(entry.isIntersecting);
      },
      { threshold: 1.0, rootMargin: "0px 0px 50px 0px" }
    );

    if (bottomRef.current) observer.observe(bottomRef.current);
    return () => observer.disconnect();
  }, []);

  React.useEffect(() => {
    if (autoScroll && bottomRef.current) {
      isScrollingRef.current = true;
      bottomRef.current.scrollIntoView({ behavior: "smooth", block: "nearest" });
      setTimeout(() => (isScrollingRef.current = false), 100);
    }
  }, [chatHistory, autoScroll]);

  return (
    <>
      {children}
      {/* <div ref={bottomRef} style={{ height: 10 }} /> */}
    </>
  );
}
