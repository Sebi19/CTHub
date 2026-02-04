import {
    AppShell,
    Group,
    Title,
    Image,
    Button,
    ActionIcon,
    useMantineColorScheme,
    useComputedColorScheme,
    Burger, Stack, Drawer
} from '@mantine/core';
import {useNavigate, useLocation, Outlet} from 'react-router-dom';
import { IconTrophy, IconSun, IconMoon } from '@tabler/icons-react';
import logo from './assets/v1.1.png';
import {useDisclosure} from "@mantine/hooks";

const NAV_LINKS = [
    { link: '/robotgame', label: 'Leaderboard', icon: IconTrophy },
    // Add more later: { link: '/teams', label: 'Teams', icon: IconUsers },
];

export default function App() {
    const navigate = useNavigate();
    const location = useLocation();
    const [opened, { toggle, close }] = useDisclosure();

    // Theme Hooks
    const { setColorScheme } = useMantineColorScheme();
    const computedColorScheme = useComputedColorScheme('light', { getInitialValueInEffect: true });

    const isActive = (path: string) => location.pathname === path;

    const toggleColorScheme = () => {
        setColorScheme(computedColorScheme === 'light' ? 'dark' : 'light');
    };

    const renderNavLinks = (isMobile: boolean) => NAV_LINKS.map((item) => (
        <Button
            key={item.link}
            variant={isActive(item.link) ? 'light' : 'subtle'}
            leftSection={<item.icon size={18} />}
            fullWidth={isMobile}
            justify={isMobile ? "flex-start" : "center"}
            onClick={() => { navigate(item.link); if(isMobile) close(); }}
        >
            {item.label}
        </Button>
    ));

    return (
        <AppShell header={{ height: 60 }} padding="0">
            <AppShell.Header>
                <Group h="100%" px="md" justify="space-between">

                    {/* LEFT: Logo & Burger */}
                    <Group>
                        <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
                        <Group gap="sm" style={{ cursor: 'pointer' }} onClick={() => navigate('/')}>
                            <Image src={logo} h={32} w="auto" fit="contain" />
                            {/* Hide full text on tiny screens */}
                            <Title order={3} visibleFrom="xs">Challenge Team Hub</Title>
                            {/* Show acronym on very small screens */}
                            <Title order={3} hiddenFrom="xs">CTH</Title>
                        </Group>
                    </Group>

                    {/* RIGHT: Actions (Theme + Auth) */}
                    <Group gap="xs">
                        {/* Desktop Nav Links (Hidden on mobile) */}
                        <Group visibleFrom="sm" gap={5} mr={"md"}>
                            {renderNavLinks(false)}
                        </Group>

                        {/* Theme Toggle */}
                        <ActionIcon onClick={toggleColorScheme} variant="default" size="lg">
                            {computedColorScheme === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}
                        </ActionIcon>

                        {/*{!isAuthenticated ? (
                            <Button variant="default" onClick={() => navigate('/login')} leftSection={<IconLogin size={18}/>}>
                                Login
                            </Button>
                        ) : (
                            <UserMenu user={user} logout={logout} />
                        )}*/}
                    </Group>
                </Group>
            </AppShell.Header>

            {/* MOBILE DRAWER */}
            <Drawer opened={opened} onClose={close} size="75%" padding="md" title="Menu" hiddenFrom="sm">
                <Stack>
                    {renderNavLinks(true)}
                    {/*{!isAuthenticated && (
                        <Button variant="default" fullWidth justify="flex-start" onClick={() => { navigate('/login'); close(); }} leftSection={<IconLogin size={18}/>}>
                            Login
                        </Button>
                    )}*/}
                </Stack>
            </Drawer>

            <AppShell.Main h="calc(100vh - 60px)">
                <Outlet />
            </AppShell.Main>
        </AppShell>
    );
}