import { ActionIcon, Box, Group, Stack, Text, ThemeIcon, Tooltip } from '@mantine/core';
import { IconCircleCheck, IconCircleDot } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import type { NotificationDto } from '../../api/generated.ts';
import { formatNotificationTime, getNotificationIcon, getNotificationText } from './notificationText.ts';
import { useNotifications } from './NotificationsContext.tsx';
import classes from './NotificationItem.module.css';

interface NotificationItemProps {
    notification: NotificationDto;
    /** Clamps the body for the bell popover. */
    compact?: boolean;
    /** Called after the item was opened, e.g. to close the popover. */
    onOpened?: () => void;
}

export function NotificationItem({ notification, compact = false, onOpened }: NotificationItemProps) {
    const { t } = useTranslation();
    const { openNotification, markRead, markUnread } = useNotifications();
    const { title, body } = getNotificationText(t, notification);
    const { icon: Icon, color } = getNotificationIcon(notification.type);
    const unread = !notification.readAt;

    const open = () => {
        openNotification(notification);
        onOpened?.();
    };

    return (
        <Box
            className={classes.item}
            data-unread={unread || undefined}
            role="button"
            tabIndex={0}
            onClick={open}
            onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    open();
                }
            }}
            px="md"
            py="sm"
        >
            <Group wrap="nowrap" align="flex-start" gap="sm">
                <ThemeIcon variant="light" color={color} radius="xl" size="lg">
                    <Icon size={18} />
                </ThemeIcon>
                <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
                    <Text size="sm" fw={unread ? 600 : 500} lineClamp={compact ? 1 : undefined}>
                        {title}
                    </Text>
                    {body && (
                        <Text size="sm" c="dimmed" lineClamp={compact ? 2 : undefined} style={{ whiteSpace: 'pre-line' }}>
                            {body}
                        </Text>
                    )}
                    <Text size="xs" c="dimmed">{formatNotificationTime(notification.createdAt)}</Text>
                </Stack>
                <Tooltip label={unread ? t('app.notifications.mark_read') : t('app.notifications.mark_unread')}>
                    <ActionIcon
                        variant="subtle"
                        color={unread ? 'blue' : 'gray'}
                        aria-label={unread ? t('app.notifications.mark_read') : t('app.notifications.mark_unread')}
                        onClick={(event) => {
                            event.stopPropagation();
                            void (unread ? markRead(notification) : markUnread(notification));
                        }}
                        onKeyDown={(event) => event.stopPropagation()}
                    >
                        {unread ? <IconCircleDot size={18} /> : <IconCircleCheck size={18} />}
                    </ActionIcon>
                </Tooltip>
            </Group>
        </Box>
    );
}
