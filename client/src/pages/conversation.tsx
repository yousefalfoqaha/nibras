import { Image, Text } from '@mantine/core';
import { UserInput } from '../components/user-input';
import styles from './conversation.module.css';
import { useChatHistory, type ChatMessage } from '../contexts/chat-history';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export function Conversation() {
  const { chatHistory } = useChatHistory();

  return (
    <main className={styles.main}>
      <section className={styles.header}>
        <Image src="../../public/logo.png" w="auto" h={40} />

        <Text size="xs" c="dimmed" ta="center">
          Friday, Dec 12 • GJUBot
        </Text>
      </section>

      <div className={styles.messages}>
        {chatHistory.map(m => (
          m.role === 'USER' ? <UserMessageBubble message={m} /> : <BotMessageMarkdown message={m} />
        ))}
      </div>

      <ChatInterface />
    </main>
  )
}

type UserMessageBubbleProps = {
  message: ChatMessage;
}

function UserMessageBubble({ message }: UserMessageBubbleProps) {
  return (
    <div className={styles.userMessageBubble}>
      {message.content}
    </div>
  )
}

type BotMessageMarkdownProps = {
  message: ChatMessage;
}

function BotMessageMarkdown({ message }: BotMessageMarkdownProps) {
  const { chatHistory } = useChatHistory();

  const isPending = chatHistory.some(m => m.status === 'PENDING' && m.id === message.id);

  if (isPending) {
    return (
      <svg width="15" height="15" viewBox="0 0 15 15" className={styles.botPending}>
        <circle
          cx="6"
          cy="6"
          r="6"
          strokeWidth="2"
        />
      </svg>
    );
  }

  return (
    <div className={styles.botMessageMarkdown}>
      <Markdown remarkPlugins={[remarkGfm]}>
        {message.content}
      </Markdown>
    </div>
  )
}

function ChatInterface() {
  return (
    <section className={styles.chatInterface}>
      <UserInput />

      <Text size="xs" c="dimmed" my="sm" ta="center">
        GJUBot can make mistakes, check with an academic advisor.
      </Text>
    </section>
  );
}
