import { Avatar, Image, Typography } from '@mantine/core';
import { UserInput } from '../components/user-input';
import styles from './conversation.module.css';
import { useChat, type ChatMessage } from '../contexts/chat-context';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { UserRound } from 'lucide-react';
import nibrasIdle from '/nibras-idle.png';
import nibrasThinking from '/nibras-thinking.png';
import nibrasAnswering from '/nibras-answering.png';
import React from 'react';

const AVATAR_IMAGES = {
  IDLE: nibrasIdle,
  THINKING: nibrasThinking,
  ANSWERING: nibrasAnswering,
};

export function Conversation() {
  const { chatHistory, lastUserMessageId } = useChat();
  const lastUserMessageRef = React.useRef<HTMLDivElement | null>(null);

  React.useEffect(() => {
    window.scrollTo({
      top: document.documentElement.scrollHeight
    });
  }, []);

  return (
    <main className={styles.conversation}>
      <section className={styles.messages}>
        {chatHistory.map((m) => {
          const isLastUserMessage = m.role === "USER" && m.id === lastUserMessageId;

          return m.role === "USER" ? (
            <UserMessageBubble key={m.id} ref={isLastUserMessage ? lastUserMessageRef : null} message={m} />
          ) : (
            <AssistantMessageMarkdown key={m.id} message={m} />
          )
        }
        )}
        <AssistantAnswerOutput />
        <AssistantAvatar />
        {lastUserMessageRef && <AnswerSpace userMessageRef={lastUserMessageRef} />}
      </section>
      <ChatInterface />
    </main>
  );
}

type AnswerSpaceProps = {
  userMessageRef: React.RefObject<HTMLDivElement | null>;
};

export function AnswerSpace({ userMessageRef }: AnswerSpaceProps) {
  const [height, setHeight] = React.useState(0);
  const { assistantState } = useChat();

  if (!userMessageRef.current) return;
  if (assistantState === 'IDLE' || assistantState === 'ANSWERING') return;

  React.useLayoutEffect(() => {
    console.log('hey')

    window.scrollTo({
      top: document.documentElement.scrollHeight,
      behavior: 'smooth'
    });

    const updateHeight = () => {
      const top = userMessageRef.current!.getBoundingClientRect().top;
      setHeight(top);
    };

    updateHeight();
    window.addEventListener("resize", updateHeight);

    return () => window.removeEventListener("resize", updateHeight);
  }, [userMessageRef]);

  console.log(height)
  return <div style={{ height }} />;
}

type UserMessageBubbleProps = {
  message: ChatMessage;
  ref: React.RefObject<HTMLDivElement | null> | null;
};

function UserMessageBubble({ message, ref }: UserMessageBubbleProps) {
  return (
    <div ref={ref} className={styles.userMessage}>
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

function AssistantAnswerOutput() {
  const { answerStream } = useChat();

  if (!answerStream) {
    return null;
  }

  return <AssistantMessageMarkdown message={{ id: 'streaming', content: answerStream, role: 'ASSISTANT' }} />
}

function AssistantAvatar() {
  const { assistantState } = useChat();

  return (
    <div
      key={assistantState}
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
