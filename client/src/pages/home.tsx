import { Button, Flex, Stack, Text, Image, type ButtonProps } from '@mantine/core';
import { Banknote, Glasses, Map } from 'lucide-react';
import { UserInput } from '../components/user-input';
import styles from './home.module.css';
import topicButtonClasses from './topic-button.module.css';
import { useChatHistory } from '../contexts/chat-history';
import { Conversation } from './conversation';

const suggestedTopics: Topic[] = [
  {
    name: 'Study plan framework',
    prompt: 'What is the framework in the study plan?',
    icon: Map
  },
  {
    name: 'Academic and registration fees',
    prompt: 'What are the admission fees',
    icon: Banknote
  },
  {
    name: 'Program admission requirements',
    prompt: 'What is the framework in the study plan?',
    icon: Map
  },
  {
    name: 'Academic calendar events',
    prompt: 'Who is the lecturer ',
    icon: Glasses
  },
  {
    name: 'Registration deadlines',
    prompt: 'Who is the lecturer ',
    icon: Glasses
  },
  {
    name: 'Study plan semester guide',
    prompt: 'Who is the lecturer ',
    icon: Glasses
  },
  {
    name: 'Course prerequisites',
    prompt: 'Who is the lecturer ',
    icon: Glasses
  },
];

export function Home() {
  return (
    <main className={styles.main}>
      <Stack>
        <Image h={125} w={125} src="nibras.png" />
        <Stack gap={0}>
          <Text>Hi, I'm <span style={{ color: 'var(--mantine-color-primary-filled)', fontWeight: 700 }}>Nibras</span></Text>
          <h1 className={styles.header}>GJU's AI assistant</h1>
        </Stack>
      </Stack>

      <Chat />

      <Disclaimer />
    </main>
  );
}

function Chat() {
  const { chatHistory } = useChatHistory();

  if (chatHistory.length === 0) {
    return (
      <section className={styles.chatInterface}>
        <UserInput />
        <Flex wrap="wrap" gap={5}>
          {suggestedTopics.map(t => <TopicButton key={t.name} topic={t} />)}
        </Flex>
      </section>
    );
  }

  return <Conversation />;
}

function Disclaimer() {
  const { chatHistory } = useChatHistory();

  if (chatHistory.length > 0) {
    return null;
  }

  return (
    <p className={styles.disclaimer}>
      Nibras can make mistakes, check with an academic advisor.
    </p>
  );
}

type Topic = {
  name: string;
  prompt: string;
  icon: React.ElementType;
};

interface TopicButtonProps extends ButtonProps {
  topic: Topic;
}

export function TopicButton({ topic }: TopicButtonProps) {
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
    >
      {topic.name}
    </Button>
  );
}
