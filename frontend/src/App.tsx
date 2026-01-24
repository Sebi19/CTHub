import { AppShell, Group, Title, Image, Button, ActionIcon, useMantineColorScheme, useComputedColorScheme } from '@mantine/core';
import { Routes, Route, useNavigate, useLocation, Navigate } from 'react-router-dom';
import { IconTrophy, IconSun, IconMoon } from '@tabler/icons-react';
import logo from './assets/fllhub.png';

// Components
import { LeaderboardPage } from './pages/LeaderboardPage';

export default function App() {
    const navigate = useNavigate();
    const location = useLocation();

    // 1. Hook to control the theme
    const { setColorScheme } = useMantineColorScheme();

    // 2. Hook to check what theme is currently active
    // 'getInitialValueInEffect: true' prevents flickering on initial load
    const computedColorScheme = useComputedColorScheme('light', { getInitialValueInEffect: true });

    const isActive = (path: string) => location.pathname === path;

    const toggleColorScheme = () => {
        setColorScheme(computedColorScheme === 'light' ? 'dark' : 'light');
    };

    return (
        <AppShell
            header={{ height: 60 }}
            padding="0"
        >
            <AppShell.Header>
                <Group h="100%" px="md" justify="space-between">
                    <Group
                        gap="sm"
                        style={{ cursor: 'pointer', userSelect: 'none' }}
                        onClick={() => navigate('/')}
                    >
                        <Image
                            src={logo}
                            h={32}
                            w="auto"
                            fit="contain"
                            alt="FLL Hub Logo"
                        />
                        <Title order={3}>FLL Hub</Title>
                    </Group>

                    <Group gap="xs">
                        <Button
                            variant={isActive('/robotgame') ? 'light' : 'subtle'}
                            leftSection={<IconTrophy size={18} />}
                            onClick={() => navigate('/robotgame')}
                        >
                            Leaderboard
                        </Button>

                        {/* 3. The Toggle Button 🌗 */}
                        <ActionIcon
                            onClick={toggleColorScheme}
                            variant="default"
                            size="lg"
                            aria-label="Toggle color scheme"
                            ml={10} // Add a little margin left to separate it from nav buttons
                        >
                            {computedColorScheme === 'dark' ? (
                                <IconSun size={18} />
                            ) : (
                                <IconMoon size={18} />
                            )}
                        </ActionIcon>
                    </Group>
                </Group>
            </AppShell.Header>

            <AppShell.Main h="calc(100vh - 60px)">
                <Routes>
                    <Route path="/" element={<Navigate to="/robotgame" replace />} />
                    <Route path="/robotgame" element={<LeaderboardPage />} />
                </Routes>
            </AppShell.Main>
        </AppShell>
    );
}