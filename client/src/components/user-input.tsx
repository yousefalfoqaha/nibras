import { ActionIcon, TextInput } from "@mantine/core";
import { SendHorizonal } from "lucide-react";
import styles from './user-input.module.css';

export function UserInput() {
  return (
    <div className={styles.userInput}>
      <TextInput variant="unstyled" style={{ width: '35rem', }} placeholder="Ask about GJU..." />

      <SendButton />
    </div>
  );

}

function SendButton() {
  return (
    <ActionIcon radius="md" color="var(--mantine-primary-color-9)" size="lg">
      <SendHorizonal size={20} />
    </ActionIcon>
  )
}
