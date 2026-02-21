import { Text, Image } from '@mantine/core';
import styles from './Header.module.css';
import nibrasLogo from '../assets/nibras.png';

export function Header() {
	return (
		<header className={styles.header}>
			<div className={styles.intro}>
				<Image h={125} w={125} src={nibrasLogo} />
				<Text>Hi, I'm <span style={{ color: 'var(--mantine-color-primary-filled)', fontWeight: 700 }}>Nibras</span></Text>
			</div>

			<h1>GJU's AI assistant</h1>
		</header>
	);
}
