import { ActionIcon, TextInput } from "@mantine/core";
import { SendHorizonal } from "lucide-react";
import styles from './user-input.module.css';
import { useChatHistory } from "../contexts/chat-history";
import React from "react";

export function UserInput() {
  const { streamBotAnswer } = useChatHistory();
  const [text, setText] = React.useState<string>('');

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();


    const trimmedText = text.trim();
    if (trimmedText === '') return;

    setText('');
    streamBotAnswer(trimmedText);
  }

  return (
    <form onSubmit={onSubmit}>
      <div className={styles.userInput}>
        <TextInput
          variant="unstyled"
          autoFocus
          value={text}
          onChange={(e) => setText(e.target.value)}
          style={{ width: '100%', }}
          placeholder="Ask about GJU..."
        />

        <SendButton />
      </div>
    </form>
  );
}

function SendButton() {
  const { isBotBusy } = useChatHistory();

  return (
    <ActionIcon disabled={isBotBusy} type="submit" radius="md" color="var(--mantine-primary-color-9)" size="lg">
      <SendHorizonal size={20} />
    </ActionIcon>
  )
}
