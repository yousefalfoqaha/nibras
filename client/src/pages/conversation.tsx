import { ActionIcon, Avatar, Image, Typography } from '@mantine/core';
import { UserInput } from '../components/user-input';
import { useChat, type ChatMessage } from '../contexts/chat-context';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { ArrowDown, UserRound } from 'lucide-react';
import nibrasIdle from '/nibras-idle.png';
import nibrasThinking from '/nibras-thinking.png';
import nibrasAnswering from '/nibras-answering.png';
import React from 'react';
import { useScroll } from '../contexts/scroll-context';
import styles from './conversation.module.css';
import { Disclaimer } from '../components/Disclaimer';
import { Header } from '../components/Header';

const AVATAR_IMAGES = {
	IDLE: nibrasIdle,
	THINKING: nibrasThinking,
	ANSWERING: nibrasAnswering,
};

export function Conversation() {
	const { chatHistory, lastUserMessageId } = useChat();
	const lastUserMessageRef = React.useRef<HTMLDivElement | null>(null);
	const { scrollToBottom } = useScroll();

	React.useEffect(() => {
		document.title = "Nibras · Chat"
		scrollToBottom('auto');
	}, []);

	React.useEffect(() => {
		scrollToBottom();
	}, [lastUserMessageId]);

	return (
		<main className={styles.conversation}>
			<Header />

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
			</section>

			<ChatInterface />
		</main>
	);
}

type UserMessageBubbleProps = {
	message: ChatMessage;
	ref: React.RefObject<HTMLDivElement | null> | null;
}

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
}

function AssistantMessageMarkdown({ message }: AssistantMessageMarkdownProps) {
	return (
		<Typography className={styles.assistantMessage}>
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
			<ScrollDownButton />

			<UserInput />

			<Disclaimer />
		</section>
	);
}

function ScrollDownButton() {
	const { answerStream, assistantState } = useChat();
	const { scrollViewportRef, scrollToBottom } = useScroll();
	const [show, setShow] = React.useState(false);
	const SHOW_THRESHOLD = 50;

	React.useEffect(() => {
		const viewport = scrollViewportRef.current;
		if (!viewport) return;

		setShow(false);
		if (assistantState === 'THINKING') return;

		const checkScroll = () => {
			const isNotAtBottom =
				viewport.clientHeight + viewport.scrollTop < viewport.scrollHeight - SHOW_THRESHOLD;

			setShow(isNotAtBottom);
		};

		viewport.addEventListener('scroll', checkScroll);
		checkScroll();

		return () => viewport.removeEventListener('scroll', checkScroll);
	}, [answerStream, assistantState, scrollViewportRef]);

	return (
		<ActionIcon
			classNames={{
				root: styles.scrollDownButton
			}}
			data-visible={show}
			variant="default"
			radius="xl"
			size="lg"
			onClick={() => scrollToBottom('smooth')}
		>
			<ArrowDown size={16} />
		</ActionIcon>
	);
}
