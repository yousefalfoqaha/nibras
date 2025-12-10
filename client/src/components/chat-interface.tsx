import { Textarea, TextInput } from "@mantine/core";
import styles from './chat-interface.module.css';

export function ChatInterface() {
  return (
    <main className={styles.main}>
      <div className={styles.content}>
        <h1 className={styles.header}>
          What can I help you with?
        </h1>

        <UserInput />
      </div>

    </main>
  );
}

function UserInput() {

  return (
    <div className={styles.textField}>
      <TextInput variant="unstyled" />
    </div>
  );

}
