import { Avatar, Image, Typography } from '@mantine/core';
import { UserInput } from '../components/user-input';
import styles from './conversation.module.css';
import { useChatHistory, type ChatMessage } from '../contexts/chat-history';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import React from 'react';
import { UserRound } from 'lucide-react';

export function Conversation() {
  const { chatHistory } = useChatHistory();

  return (
    <main className={styles.conversation}>
      <AutoScroll>
        <div className={styles.messages}>
          {chatHistory.map(m => (
            m.role === 'USER' ?
              <UserMessageBubble message={m} /> :
              <AssistantMessageMarkdown message={m} />
          ))}
        </div>
      </AutoScroll>


      <ChatInterface />
    </main>
  );
}

type UserMessageBubbleProps = {
  message: ChatMessage;
}

function UserMessageBubble({ message }: UserMessageBubbleProps) {
  return (
    <div className={styles.userMessage}>
      <Avatar
        color="var(--mantine-color-primary-filled)"
        variant="transparent"
      >
        <UserRound />
      </Avatar>

      {message.content}
    </div>
  )
}

type BotMessageMarkdownProps = {
  message: ChatMessage;
}

function AssistantMessageMarkdown({ message }: BotMessageMarkdownProps) {
  const { chatHistory } = useChatHistory();
  const lastAssistantMessage = [...chatHistory]
    .reverse()
    .find(m => m.role === 'ASSISTANT');
  const isLastAssistantMessage = lastAssistantMessage?.id === message.id;
  const isPending = chatHistory.some(m => m.status === 'PENDING' && m.id === message.id);
  const isAnswering = chatHistory.some(m => m.status === 'STREAMING' && m.id === message.id);

  let avatarState = "standing";

  if (isPending) {
    avatarState = "thinking";
  } else if (isAnswering) {
    avatarState = "talking";
  }

  const avatarSrc = `nibras-${avatarState}.png`;

  if (isPending) {
    return (
      <div className={styles.assistantAvatar} data-thinking={true}>
        <Image
          key={avatarState}
          w={100}
          h="auto"
          src={avatarSrc}
        />
      </div>
    );
  }

  return (
    <div className={styles.assistantMessage}>
      <div className={styles.assistantMessageMarkdown}>
        <Typography>
          <Markdown remarkPlugins={[remarkGfm]}>
            {message.content}
          </Markdown>
        </Typography>
      </div>
      {isLastAssistantMessage && (
        <div className={styles.assistantAvatar}>
          <Image
            key={avatarState}
            w={100}
            h="auto"
            src={avatarSrc}
          />
        </div>
      )}
    </div>
  )
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
  const { chatHistory } = useChatHistory();
  const bottomRef = React.useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = React.useState(true);
  const isScrollingRef = React.useRef(false);

  // Observe when bottom marker is visible
  React.useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        // Only update if we're not actively scrolling
        if (!isScrollingRef.current) {
          setAutoScroll(entry.isIntersecting);
        }
      },
      {
        threshold: 1.0, // Fully visible
        rootMargin: "0px 0px 50px 0px" // Add buffer at bottom
      }
    );

    if (bottomRef.current) {
      observer.observe(bottomRef.current);
    }

    return () => observer.disconnect();
  }, []);

  // Auto-scroll when chat updates and autoScroll is true
  React.useEffect(() => {
    if (autoScroll && bottomRef.current) {
      isScrollingRef.current = true;

      bottomRef.current.scrollIntoView({
        behavior: "smooth", // Changed to auto - smooth can cause issues
        block: "nearest"
      });

      // Reset scrolling flag
      setTimeout(() => {
        isScrollingRef.current = false;
      }, 100);
    }
  }, [chatHistory, autoScroll]);

  // <div ref={bottomRef} style={{ height: 10 }} /> {/* Made taller */}
  return (
    <>
      {children}
    </>
  );
}
