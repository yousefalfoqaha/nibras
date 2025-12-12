import { Image, Text } from '@mantine/core';
import { UserInput } from '../components/user-input';
import styles from './conversation.module.css';
import { useChatHistory, type ChatMessage } from '../contexts/chat-history';

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
      <section className={styles.messages}>
        {chatHistory.map(m => (
          m.role === 'user' ? <UserMessageBubble message={m} /> : <BotMessageMarkdown message={m} />
        ))}
      </section>

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

  return (
    <div className={styles.botMessageMarkdown}>
      {message.content}
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
