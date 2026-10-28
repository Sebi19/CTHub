import { useState } from 'react';
import { Affix, Button, Group, Paper, Stack, Text, ThemeIcon, Transition } from '@mantine/core';
import { notifications as toasts } from '@mantine/notifications';
import { IconBellRinging } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { useNotifications } from '../NotificationsContext.tsx';
import { usePush } from './PushContext.tsx';
import { isPromptSnoozed, isStandalone, snoozePrompt } from './pushSupport.ts';

/**
 * One-time offer to enable push, shown only in the installed app: installing is a strong signal
 * that the user wants notifications, whereas a browser tab only offers push on the settings page.
 */
export function PushPromptBanner() {
    const { t } = useTranslation();
    const { enabled } = useNotifications();
    const { supported, available, permission, subscribed, ready, busy, enable } = usePush();
    const [dismissed, setDismissed] = useState(() => isPromptSnoozed());

    const visible = enabled && supported && available === true && ready && !subscribed && !dismissed
        && permission !== 'denied' && isStandalone();

    const onEnable = async () => {
        try {
            await enable();
        } catch {
            toasts.show({ color: 'red', message: t('app.notifications.push.enable_error') });
        }
        // Granted, denied or dismissed: either way the question has been answered for now.
        snoozePrompt();
        setDismissed(true);
    };

    const onLater = () => {
        snoozePrompt();
        setDismissed(true);
    };

    return (
        <Affix position={{ bottom: 16, left: 16, right: 16 }} zIndex={300}>
            <Transition mounted={visible} transition="slide-up">
                {(styles) => (
                    <Paper withBorder shadow="lg" radius="md" p="md" maw={480} mx="auto" style={styles}>
                        <Group align="flex-start" wrap="nowrap" gap="md">
                            <ThemeIcon variant="light" size="lg" radius="xl">
                                <IconBellRinging size={18} />
                            </ThemeIcon>
                            <Stack gap="xs" style={{ flex: 1 }}>
                                <Text fw={600}>{t('app.notifications.push.prompt.title')}</Text>
                                <Text size="sm" c="dimmed">{t('app.notifications.push.prompt.body')}</Text>
                                <Group gap="xs" justify="flex-end">
                                    <Button variant="subtle" color="gray" size="xs" onClick={onLater} disabled={busy}>
                                        {t('app.notifications.push.prompt.later')}
                                    </Button>
                                    <Button size="xs" onClick={() => void onEnable()} loading={busy}>
                                        {t('app.notifications.push.prompt.enable')}
                                    </Button>
                                </Group>
                            </Stack>
                        </Group>
                    </Paper>
                )}
            </Transition>
        </Affix>
    );
}
