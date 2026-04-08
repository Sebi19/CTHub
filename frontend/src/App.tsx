import {
    AppShell,
    Group,
    Title,
    Image,
    Button,
    ActionIcon,
    useMantineColorScheme,
    useComputedColorScheme,
    Burger, Stack, Drawer, Menu, Avatar, Text, rem, Tooltip, Box, Divider, Anchor, Flex, Container, Badge
} from '@mantine/core';
import {useNavigate, useLocation, Outlet} from 'react-router-dom';
import {
    IconTrophy,
    IconSun,
    IconMoon,
    IconLogin,
    IconChevronDown,
    IconLogout,
    IconUser, IconSearch, IconBrandGithub
} from '@tabler/icons-react';
import logo from './assets/CTH.svg';
import {useDisclosure} from "@mantine/hooks";
import {useAuth} from "./features/auth/AuthContext.tsx";
import type {UserDto} from "./api/generated.ts";
import {LanguageSwitcher} from "./features/navbar/LanguageSwitcher.tsx";
import {Trans, useTranslation} from "react-i18next";
import {GlobalSearchSpotlight} from "./features/globalSearch/GlobalSearchSpotlight.tsx";
import {spotlight} from "@mantine/spotlight";



export default function App() {
    const { t } = useTranslation();

    const NAV_LINKS = [
        { link: '/leaderboard', label: t("app.header.leaderboard"), icon: IconTrophy },
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
                            <Avatar radius="xl" size={26} color="blue" />
                            <Text visibleFrom="sm" size="sm" fw={500}>{user.email}</Text>
                        </Group>
                    </Button>
                </Menu.Target>

                <Menu.Dropdown>
                    <Menu.Item
                        hiddenFrom={"lg"}
                        leftSection={<IconUser size={14} />}
                        style={{ opacity: 1, cursor: 'default', color: 'var(--mantine-color-text)' }}
                        // We use 'component="div"' so it doesn't behave like a button
                        component="div"
                    >{user.email}</Menu.Item>
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
            <GlobalSearchSpotlight />

            <AppShell.Header>
                <Group h="100%" px="md" justify="space-between">

                    {/* LEFT: Logo & Burger */}
                    <Group>
                        <Burger opened={opened} onClick={toggle} hiddenFrom="lg" size="sm" />
                        <Group gap="sm" style={{ cursor: 'pointer' }} onClick={() => navigate('/')}>
                            <Image src={logo} h={32} w="auto" fit="contain" />
                            {/* Hide full text on tiny screens */}
                            <Title order={3} visibleFrom="xs">Challenge Team Hub</Title>
                            {/* Show acronym on very small screens */}
                            <Title order={3} hiddenFrom="xs">CTH</Title>
                        </Group>
                        <Anchor href="https://github.com/Sebi19/CTHub" target="_blank" td="none">
                            <ActionIcon variant="default" size="lg">
                                <IconBrandGithub size={18} />
                            </ActionIcon>
                        </Anchor>
                    </Group>

                    {/* RIGHT: Actions (Theme + Auth) */}
                    <Group gap="xs">
                        <Button
                            variant="default"
                            leftSection={<IconSearch size={16} />}
                            rightSection={<Badge size="xs" variant="filled" color="gray">{t("app.search.shortcut")}</Badge>}
                            onClick={spotlight.open}
                            visibleFrom="sm"
                        >
                            {t("app.search.button_placeholder")}
                        </Button>

                        {/* A compact icon-only button for mobile screens */}
                        <ActionIcon variant="default" size="lg" hiddenFrom="sm" onClick={spotlight.open}>
                            <IconSearch size={18} />
                        </ActionIcon>

                        {/* Desktop Nav Links (Hidden on mobile) */}
                        <Group visibleFrom="lg" gap={5} mr={"md"}>
                            {renderNavLinks(false)}
                        </Group>

                        {!(isAuthenticated && user) ? (
                            <>
                                <Button
                                    visibleFrom={"sm"}
                                    variant="default"
                                    onClick={() => navigate('/login')}
                                    leftSection={<IconLogin size={18} />}
                                >{t("app.header.login")}
                                </Button>
                                <Button hiddenFrom={"sm"} variant="default" onClick={() => navigate('/login')} p="xs">
                                    <IconLogin size={18} />
                                </Button>
                            </>

                        ) : (
                            <UserMenu user={user} logout={handleLogout} />
                        )}

                        {/* Theme Toggle */}
                        <Group visibleFrom="lg">
                            <Tooltip label={t("app.header.toggleTheme")}>
                                <ActionIcon onClick={toggleColorScheme} variant="default" size="lg">
                                    {computedColorScheme === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}
                                </ActionIcon>
                            </Tooltip>
                        </Group>


                        {/* Language Switcher*/}
                        <Group visibleFrom="lg">
                            <LanguageSwitcher isMobile={false}/>
                        </Group>
                    </Group>
                </Group>
            </AppShell.Header>

            {/* MOBILE DRAWER */}
            <Drawer opened={opened} onClose={close} size="75%" padding="md" title={t("app.sidebar.menu")} hiddenFrom="lg">
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

            <AppShell.Main>
                <Stack justify="space-between" style={{ minHeight: 'calc(100dvh - 60px)' }} gap={0}>

                    {/* Page Content Wrapper */}
                    <Box style={{ flexGrow: 1 }}>
                        <Outlet />
                    </Box>

                    {/* Footer Container */}
                    <Box component="footer" w="100%" mt="auto">
                        <Divider />
                        <Container size="xl" p="md">
                            <Flex
                                direction={{ base: 'column', md: 'row' }}
                                justify="space-between"
                                align={{ base: 'center', md: 'flex-start' }}
                                gap="xl"
                            >
                                {/* Left Side: Information Stack */}
                                <Stack gap="xs" maw={750} ta={{ base: 'center', md: 'left' }}>

                                    {/* 1. App Identity & Copyright */}
                                    <Text size="sm" fw={600} c="var(--mantine-color-text)">
                                        {t("app.footer.app_name")} <Text component="span" fw={400} size="xs" c="dimmed">{t("app.footer.copyright", {year: new Date().getFullYear()})}</Text>
                                    </Text>

                                    {/* 2. Data Source */}
                                    <Text size="xs" c="dimmed">
                                        <Trans
                                            i18nKey="app.footer.data_source"
                                            t={t}
                                            components={{
                                                a: (
                                                    <Anchor
                                                        href={"https://www.first-lego-league.org/"}
                                                        target="_blank"
                                                        td="underline"
                                                    />
                                                )
                                            }}
                                        />
                                    </Text>

                                    {/* 3. FIRST Disclaimer (The actual fine print) */}
                                    <Text c="dimmed" style={{ fontSize: '0.65rem', lineHeight: 1.4 }}>
                                        <i>FIRST</i>® LEGO® League is a jointly held trademark of <i>FIRST</i>® (
                                        <Anchor href="https://www.firstinspires.org" target="_blank" c="dimmed" td="underline" style={{ fontSize: 'inherit' }}>
                                            www.firstinspires.org
                                        </Anchor>
                                        ) and the LEGO Group, <i>neither of which is overseeing, involved with, or responsible for this activity, product, or service.</i>
                                    </Text>
                                </Stack>

                                {/* Right Side: Links */}
                                <Group gap="xl" pt={{ base: 0, md: 2 }}>
                                    <Anchor size="sm" c="dimmed" onClick={() => navigate('/impressum')}>
                                        {t("app.footer.imprint")}
                                    </Anchor>
                                    <Anchor size="sm" c="dimmed" onClick={() => navigate('/privacy')}>
                                        {t("app.footer.privacy")}
                                    </Anchor>
                                </Group>
                            </Flex>
                        </Container>
                    </Box>

                </Stack>
            </AppShell.Main>
        </AppShell>
    );
}