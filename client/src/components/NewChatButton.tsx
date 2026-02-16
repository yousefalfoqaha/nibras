import { ActionIcon, Tooltip } from '@mantine/core';
import { SquarePen } from 'lucide-react';
import { useChat } from '../contexts/chat-context';
import styles from './NewChatButton.module.css';

export function NewChatButton() {
  const { newChat, chatHistory } = useChat();

  return (
    <Tooltip label="New chat" position="left">
      <ActionIcon
        variant="default"
        radius="lg"
        size="xl"
        data-visible={chatHistory.length > 0}
        className={styles.newChatButton}
        onClick={newChat}
      >
        <SquarePen size={20} />
      </ActionIcon>
    </Tooltip>
  );
}