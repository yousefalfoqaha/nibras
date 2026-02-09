import { Button, Flex, Stack, Text, Image, type ButtonProps, ActionIcon, Tooltip } from '@mantine/core';
import { Banknote, BookOpen, Calendar, Clock, GraduationCap, Map, ScrollText, SquarePen } from 'lucide-react';
import { UserInput } from '../components/user-input';
import styles from './home.module.css';
import topicButtonClasses from './topic-button.module.css';
import { useChat } from '../contexts/chat-context';
import { Conversation } from './conversation';
import React from 'react';

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
    <>
      <nav className={styles.navbar}>
        <NewChatButton />
      </nav>
      <section className={styles.app}>
        <Stack>
          <Image h={125} w={125} src="nibras.png" />
          <Stack gap={0}>
            <Text>Hi, I'm <span style={{ color: 'var(--mantine-color-primary-filled)', fontWeight: 700 }}>Nibras</span></Text>
            <h1 className={styles.header}>GJU's AI assistant</h1>
          </Stack>
        </Stack>

        <Chat />

        <Disclaimer />
      </section>
    </>
  );
}

function Chat() {
  const { chatHistory, assistantState } = useChat();

  if (chatHistory.length === 0 && assistantState === 'IDLE') {
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

function NewChatButton() {
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

function Disclaimer() {
  const { chatHistory, assistantState } = useChat();

  if (chatHistory.length === 0 && assistantState === 'IDLE') {
    return (
      <p className={styles.disclaimer}>
        Nibras can make mistakes, check with an academic advisor.
      </p>
    );
  }
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
  const { prompt } = useChat();

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

        window.scrollTo({
          top: document.documentElement.scrollHeight,
          behavior: 'smooth'
        });
      }}
    >
      {topic.name}
    </Button>
  );
}
