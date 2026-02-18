import { ActionIcon, Button, Flex, Tooltip, type ButtonProps } from '@mantine/core';
import { Header } from '../components/Header';
import { UserInput } from '../components/user-input';
import styles from './home.module.css';
import { Disclaimer } from '../components/Disclaimer';
import { useChat } from '../contexts/chat-context';
import { Banknote, Map, BookOpen, Calendar, Clock, GraduationCap, ScrollText, SquarePen } from 'lucide-react';
import { useScroll } from '../contexts/scroll-context';
import topicButtonClasses from './topic-button.module.css'

type Topic = {
	name: string;
	prompt: string;
	icon: React.ElementType;
};

const suggestedTopics: Topic[] = [
	{
		name: 'Study plan framework',
		prompt: 'What is the framework in the study plan?',
		icon: ScrollText
	},
	{
		name: 'Academic and registration fees',
		prompt: 'What are the admission fees',
		icon: Banknote
	},
	{
		name: 'Program admission requirements',
		prompt: 'What is the framework in the study plan?',
		icon: GraduationCap
	},
	{
		name: 'Academic calendar events',
		prompt: 'Who is the lecturer ',
		icon: Calendar
	},
	{
		name: 'Registration deadlines',
		prompt: 'Who is the lecturer ',
		icon: Clock
	},
	{
		name: 'Study plan semester guide',
		prompt: 'Who is the lecturer ',
		icon: Map
	},
	{
		name: 'Course prerequisites',
		prompt: 'Who is the lecturer ',
		icon: BookOpen
	},
];

export function Home() {
	return (
		<div className={styles.home}>
			<Header />

			<section className={styles.startChatInterface}>
				<UserInput />

				<Flex wrap="wrap" gap={5}>
					{suggestedTopics.map(t => <TopicButton key={t.name} topic={t} />)}
				</Flex>
			</section>

			<Disclaimer />
		</div>
	);
}

interface TopicButtonProps extends ButtonProps {
	topic: Topic;
}

function TopicButton({ topic }: TopicButtonProps) {
	const { prompt } = useChat();
	const { scrollToBottom } = useScroll();

	const Icon = topic.icon;

	return (
		<Button classNames={{
			root: topicButtonClasses.root,
			section: topicButtonClasses.section,
			label: topicButtonClasses.label,
			inner: topicButtonClasses.inner
		}}
			leftSection={<Icon size={14} />}
			size="xs"
			onClick={() => {
				prompt(topic.name);
				scrollToBottom();
			}}
		>
			{topic.name}
		</Button>
	);
}
