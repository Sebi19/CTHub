import { useState } from 'react';
import { ActionIcon, Button, Divider, Group, Indicator, Popover, ScrollArea, Stack, Text, Tooltip } from '@mantine/core';
import { IconBell, IconChecks, IconSettings } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { NOTIFICATION_SETTINGS_PATH, NOTIFICATIONS_PAGE_PATH, useNotifications } from './NotificationsContext.tsx';
import { NotificationItem } from './NotificationItem.tsx';

const formatCount = (count: number) => (count > 99 ? '99+' : String(count));

/** Header bell. Opening it does not mark anything as read; only explicit clicks do. */
export function NotificationBell() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { enabled, unreadCount, recent, markAllRead } = useNotifications();
    const [opened, setOpened] = useState(false);

    if (!enabled) {
        return null;
    }

    return (
        <Popover opened={opened} onChange={setOpened} width={380} position="bottom-end" shadow="md" withinPortal>
            <Popover.Target>
                <Indicator
                    label={formatCount(unreadCount)}
                    size={18}
                    offset={4}
                    color="red"
                    disabled={unreadCount === 0}
                    inline
                >
                    <Tooltip label={t('app.notifications.bell_label')} disabled={opened}>
                        <ActionIcon
                            variant="default"
                            size="lg"
                            aria-label={t('app.notifications.bell_label')}
                            onClick={() => setOpened((o) => !o)}
                        >
                            <IconBell size={18} />
                        </ActionIcon>
                    </Tooltip>
                </Indicator>
            </Popover.Target>

            <Popover.Dropdown p={0} maw="calc(100vw - 16px)">
                <Group justify="space-between" px="md" py="xs">
                    <Text fw={600}>{t('app.notifications.title')}</Text>
                    <Group gap={4} wrap="nowrap">
                        <Button
                            variant="subtle"
                            size="compact-sm"
                            leftSection={<IconChecks size={16} />}
                            disabled={unreadCount === 0}
                            onClick={() => void markAllRead()}
                        >
                            {t('app.notifications.mark_all_read')}
                        </Button>
                        <Tooltip label={t('app.notifications.settings.title')}>
                            <ActionIcon
                                variant="subtle"
                                color="gray"
                                aria-label={t('app.notifications.settings.title')}
                                onClick={() => {
                                    setOpened(false);
                                    navigate(NOTIFICATION_SETTINGS_PATH);
                                }}
                            >
                                <IconSettings size={16} />
                            </ActionIcon>
                        </Tooltip>
                    </Group>
                </Group>
                <Divider />

                {recent.length === 0 ? (
                    <Text c="dimmed" size="sm" ta="center" py="xl">{t('app.notifications.empty')}</Text>
                ) : (
                    <ScrollArea.Autosize mah={420} type="auto">
                        <Stack gap={0}>
                            {recent.map((notification) => (
                                <NotificationItem
                                    key={notification.id}
                                    notification={notification}
                                    compact
                                    onOpened={() => setOpened(false)}
                                />
                            ))}
                        </Stack>
                    </ScrollArea.Autosize>
                )}

                <Divider />
                <Button
                    variant="subtle"
                    fullWidth
                    radius={0}
                    onClick={() => {
                        setOpened(false);
                        navigate(NOTIFICATIONS_PAGE_PATH);
                    }}
                >
                    {t('app.notifications.show_all')}
                </Button>
            </Popover.Dropdown>
        </Popover>
    );
}
