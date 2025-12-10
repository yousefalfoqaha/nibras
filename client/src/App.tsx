import { MantineProvider } from "@mantine/core";
import { ChatInterface } from "./components/chat-interface";
import '@mantine/core/styles.css';

function App() {
  return (
    <MantineProvider>
      <ChatInterface />
    </MantineProvider>
  );
}

export default App;
