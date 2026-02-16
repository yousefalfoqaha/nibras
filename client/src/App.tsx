import { createTheme, MantineProvider, type CSSVariablesResolver } from "@mantine/core";
import '@mantine/core/styles.css';
import { ChatProvider, useChat } from "./contexts/chat-context";
import { Suspense } from "react";
import { preload } from "react-dom";
import React from "react";
import { ScrollProvider } from "./contexts/scroll-context";
import styles from './app.module.css'
import { Home } from "./pages/home";
import { Conversation } from "./pages/conversation";

function App() {
	const viewportRef = React.useRef(null);

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
				<ScrollProvider scrollViewportRef={viewportRef}>
					<Suspense fallback={"Loading..."}>
						<div className={styles.viewport} ref={viewportRef}>
							<Nibras />
						</div>
					</Suspense>
				</ScrollProvider>
			</ChatProvider>
		</MantineProvider>
	);
}

function Nibras() {
	const { chatHistory, assistantState } = useChat();

	return (
		<div className={styles.nibras}>
			{chatHistory.length === 0 && assistantState === 'IDLE' ? <Home /> : <Conversation />}
		</div>
	)
}

export default App;
