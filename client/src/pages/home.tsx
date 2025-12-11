import { Button, Flex, Group, Stack, Text, Title, Image, type ButtonProps } from '@mantine/core';
import { Banknote, BookOpen, Bot, Glasses, Map } from 'lucide-react';
import { UserInput } from '../components/user-input';
import styles from './home.module.css';
import topicButtonClasses from './topic-button.module.css';

export function Home() {
  const suggestedTopics: Topic[] = [
    {
      name: 'Study plans',
      prompt: 'What is the basic study plan for ',
      icon: Map
    },
    {
      name: 'Admission fees',
      prompt: 'What are the admission fees',
      icon: Banknote
    },
    {
      name: 'Past exams',
      prompt: 'Give me past exam questions for the course ',
      icon: BookOpen
    },
    {
      name: 'Lecturers',
      prompt: 'Who is the lecturer ',
      icon: Glasses
    }
  ];

  return (
    <main className={styles.main}>
      <Stack gap="2.5rem" align="center" justify="center" mb="10rem">
        <Stack gap="xs" align="center" justify="center">
          <Image h={70} w="auto" src="../../public/logo.png" />

          <Title>What can I help with?</Title>
        </Stack>

        <UserInput />

        <Stack align="center">
          <Group gap="xs">
            <Bot size={16} style={{ color: 'var(--mantine-primary-color-filled)' }} />

            <Text size="xs" c="dimmed">Suggested topics</Text>
          </Group>

          <Flex gap="lg">
            {suggestedTopics.map(t => <TopicButton key={t.name} topic={t} />)}
          </Flex>
        </Stack>
      </Stack>
    </main>
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

export function TopicButton({ topic, ...props }: TopicButtonProps) {
  const Icon = topic.icon;

  return (
    <Button {...props} classNames={topicButtonClasses} leftSection={<Icon size={16} />}>

      {topic.name}
    </Button>
  );
}
