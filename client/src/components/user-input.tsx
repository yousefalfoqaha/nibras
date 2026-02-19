import { ActionIcon, Textarea } from "@mantine/core";
import { SendHorizonal } from "lucide-react";
import styles from './user-input.module.css';
import { useChat } from "../contexts/chat-context";
import React from "react";
import sendButtonClasses from "./send-button.module.css";

export function UserInput() {
	const { chatHistory, prompt } = useChat();
	const [text, setText] = React.useState<string>('');
	const isDesktop = window.innerWidth >= 1024;

	const sendMessage = () => {
		const trimmedText = text.trim();
		if (trimmedText === '') return;

		setText('');
		prompt(trimmedText);

		window.scrollTo({
			top: document.documentElement.scrollHeight,
			behavior: 'smooth'
		});
	};

	const onSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		sendMessage();
	};

	const onKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
		if (event.key === 'Enter' && !event.shiftKey && isDesktop) {
			event.preventDefault();
			if (event.nativeEvent.isComposing) return;

			sendMessage();
		}
	};

	return (
		<form onSubmit={onSubmit} autoComplete="off">
			<div className={styles.userInput}>
				<Textarea
					autosize
					minRows={1}
					maxRows={8}
					variant="unstyled"
					className={styles.textArea}
					size="md"
					autoComplete="off"
					autoFocus={isDesktop}
					value={text}
					onChange={(e) => setText(e.target.value)}
					onKeyDown={onKeyDown}
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
			unstyled
			classNames={sendButtonClasses}
			data-disabled={assistantState === 'THINKING' || assistantState === 'ANSWERING' || !isPromptValid}
			type="submit"
		>
			<SendHorizonal size={20} />
		</ActionIcon>
	)
}
