import { ActionIcon, TextInput } from "@mantine/core";
import { SendHorizonal } from "lucide-react";
import styles from './user-input.module.css';
import { useChat } from "../contexts/chat-context";
import React from "react";
import sendButtonClasses from "./send-button.module.css";

export function UserInput() {
  const { chatHistory, prompt } = useChat();
  const [text, setText] = React.useState<string>('');

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedText = text.trim();
    if (trimmedText === '') return;

    setText('');

    prompt(trimmedText);

    window.scrollTo({
      top: document.documentElement.scrollHeight,
      behavior: 'smooth'
    });
  }

  return (
    <form onSubmit={onSubmit}>
      <div className={styles.userInput}>
        <TextInput
          variant="unstyled"
          autoFocus
          autoComplete="off"
          value={text}
          onChange={(e) => setText(e.target.value)}
          style={{ width: '100%' }}
          placeholder={chatHistory.length > 0 ? 'Ask a follow-up...' : 'Ask Nibras...'}
        />

        <SendButton isPromptValid={text.trim() !== ''} />
      </div>
    </form>
  );
}

type SendButtonProps = {
  isPromptValid: boolean;
}

function SendButton({ isPromptValid }: SendButtonProps) {
  const { assistantState } = useChat();

  return (
    <ActionIcon
      classNames={sendButtonClasses}
      data-disabled={assistantState === 'THINKING' || assistantState === 'ANSWERING' || !isPromptValid}
      type="submit"
    >
      <SendHorizonal size={20} />
    </ActionIcon>
  )
}
