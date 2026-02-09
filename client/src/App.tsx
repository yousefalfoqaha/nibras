import { createTheme, MantineProvider, type CSSVariablesResolver } from "@mantine/core";
import { Home } from "./pages/home";
import '@mantine/core/styles.css';
import { ChatProvider } from "./contexts/chat-context";
import { Suspense } from "react";
import { preload } from "react-dom";

function App() {
  const theme = createTheme({
    colors: {
      primary: [
        "#e9f6ff",
        "#d7e8f8",
        "#adcfed",
        "#81b5e4",
        "#5d9fdb",
        "#4691d7",
        "#398ad6",
        "#2a77be",
        "#1f6bad",
        "#035b98"
      ],
      gray: [
        "#f6f7f8",
        "#e7e7e7",
        "#cccccc",
        "#afb1b3",
        "#96999d",
        "#858a90",
        "#7c838b",
        "#697179",
        "#5c646d",
        "#4c5762"
      ]
    },
    primaryColor: 'primary',
    fontFamily: 'Inter',
    fontSizes: {
      xs: '0.75rem',
    },
    radius: {
      md: '0.75rem'
    }
  });

  const resolver: CSSVariablesResolver = (theme) => ({
    variables: {},
    light: {
      '--mantine-color-default-border': theme.colors.gray[1],
      '--mantine-color-text': '#333434'
    },
    dark: {}
  })

  preload("/nibras.png", { as: 'image' });
  preload("/nibras-idle.png", { as: 'image' });
  preload("/nibras-thinking.png", { as: 'image' });
  preload("/nibras-answering.png", { as: 'image' });

  return (
    <MantineProvider theme={theme} cssVariablesResolver={resolver}>
      <ChatProvider>
        <Suspense fallback={"Loading..."}>
          <Home />
        </Suspense>
      </ChatProvider>
    </MantineProvider>
  );
}

export default App;
