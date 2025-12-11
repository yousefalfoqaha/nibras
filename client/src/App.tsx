import { createTheme, MantineProvider, type CSSVariablesResolver } from "@mantine/core";
import { Home } from "./pages/home";
import styles from './gjubot.module.css';
import '@mantine/core/styles.css';

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
    light: {
      '--mantine-color-default-border': theme.colors.gray[1]
    },
  })

  return (
    <MantineProvider theme={theme} cssVariablesResolver={resolver}>
      <GJUBot />
    </MantineProvider>
  );
}

function GJUBot() {
  return (
    <main className={styles.main}>
      <Home />

      <p className={styles.disclaimer}>GJUBot can make mistakes, check with an academic advisor.</p>
    </main>
  );
}

export default App;
