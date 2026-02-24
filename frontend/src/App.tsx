import {
    AppShell,
    Group,
    Title,
    Image,
    Button,
    ActionIcon,
    useMantineColorScheme,
    useComputedColorScheme,
    Burger, Stack, Drawer, Menu, Avatar, Text, rem, Tooltip
} from '@mantine/core';
import {useNavigate, useLocation, Outlet} from 'react-router-dom';
import {
    IconTrophy,
    IconSun,
    IconMoon,
    IconLogin,
    IconChevronDown,
    IconLogout,
    IconUser
} from '@tabler/icons-react';
import logo from './assets/CTH.svg';
import {useDisclosure} from "@mantine/hooks";
import {useAuth} from "./features/auth/AuthContext.tsx";
import type {UserDto} from "./api/generated.ts";
import {LanguageSwitcher} from "./features/navbar/LanguageSwitcher.tsx";
import {useTranslation} from "react-i18next";



export default function App() {
    const { t } = useTranslation();

    const NAV_LINKS = [
        { link: '/robotgame', label: t("app.header.leaderboard"), icon: IconTrophy },
        // Add more later: { link: '/teams', label: 'Teams', icon: IconUsers },
    ];

    const navigate = useNavigate();
    const location = useLocation();
    const [opened, { toggle, close }] = useDisclosure();

    // Theme Hooks
    const { setColorScheme } = useMantineColorScheme();
    const computedColorScheme = useComputedColorScheme('light', { getInitialValueInEffect: true });

    const { isAuthenticated, user, logout } = useAuth();

    const isActive = (path: string) => location.pathname === path;

    const toggleColorScheme = () => {
        setColorScheme(computedColorScheme === 'light' ? 'dark' : 'light');
    };

    const handleLogout = () => {
        logout();
        navigate('/login');
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

    function UserMenu({ user, logout }: { user: UserDto, logout: () => void }) {
        return (
            <Menu shadow="md" width={200}>
                <Menu.Target>
                    <Button variant="subtle" rightSection={<IconChevronDown size={14} />} px="xs">
                        <Group gap={8}>
                            <Avatar src={user?.email} radius="xl" size={26} color="blue" />
                            <Text visibleFrom="sm" size="sm" fw={500}>{user?.email}</Text>
                        </Group>
                    </Button>
                </Menu.Target>

                <Menu.Dropdown>
                    <Menu.Item
                        hiddenFrom={"sm"}
                        leftSection={<IconUser size={14} />}
                        style={{ opacity: 1, cursor: 'default', color: 'var(--mantine-color-text)' }}
                        // We use 'component="div"' so it doesn't behave like a button
                        component="div"
                    >{user?.email}</Menu.Item>
                    <Menu.Label>Account</Menu.Label>
                    <Menu.Divider />
                    <Menu.Item
                        color="red"
                        leftSection={<IconLogout style={{ width: rem(14), height: rem(14) }} />}
                        onClick={logout}
                    >
                        Logout
                    </Menu.Item>
                </Menu.Dropdown>
            </Menu>
        );
    }

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

                        {!isAuthenticated ? (
                            <Button variant="default" onClick={() => navigate('/login')} leftSection={<IconLogin size={18}/>}>
                                {t("app.header.login")}
                            </Button>
                        ) : (
                            <UserMenu user={user!} logout={handleLogout} />
                        )}

                        {/* Theme Toggle */}
                        <Group visibleFrom="sm">
                            <Tooltip label={t("app.header.toggleTheme")}>
                                <ActionIcon onClick={toggleColorScheme} variant="default" size="lg">
                                    {computedColorScheme === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}
                                </ActionIcon>
                            </Tooltip>
                        </Group>


                        {/* Language Switcher*/}
                        <Group visibleFrom="sm">
                            <LanguageSwitcher isMobile={false}/>
                        </Group>
                    </Group>
                </Group>
            </AppShell.Header>

            {/* MOBILE DRAWER */}
            <Drawer opened={opened} onClose={close} size="75%" padding="md" title={t("app.sidebar.menu")} hiddenFrom="sm">
                <Stack justify={"space-between"} style={{ height: 'calc(100dvh - 80px)' }}>
                    <Stack gap={15}>
                        {!isAuthenticated && (
                            <Button variant="default" fullWidth justify="flex-start" onClick={() => { navigate('/login'); close(); }} leftSection={<IconLogin size={18}/>}>
                                {t("app.sidebar.login")}
                            </Button>
                        )}

                        <Stack gap={5}>
                            {renderNavLinks(true)}
                        </Stack>
                    </Stack>
                    <Stack gap={5}>
                        <LanguageSwitcher isMobile={true} />
                        {/* Theme Toggle */}
                        <Button variant="default" fullWidth justify="flex-start" onClick={toggleColorScheme} leftSection={computedColorScheme === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}>
                            {t("app.sidebar.toggleTheme")}
                        </Button>
                    </Stack>

                </Stack>
            </Drawer>

            <AppShell.Main h="calc(100vh - 60px)">
                <Outlet />
            </AppShell.Main>
        </AppShell>
    );
}