import { ActionIcon, TextInput } from "@mantine/core";
import { SendHorizonal } from "lucide-react";
import styles from './user-input.module.css';
import { useChatHistory } from "../contexts/chat-history";
import React from "react";
import sendButtonClasses from "./send-button.module.css";

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
          style={{ width: '100%' }}
          placeholder="Ask Nibras..."
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
  const { isBotBusy } = useChatHistory();

  return (
    <ActionIcon
      classNames={sendButtonClasses}
      data-disabled={isBotBusy || !isPromptValid}
      type="submit"
    >
      <SendHorizonal size={20} />
    </ActionIcon>
  )
}
