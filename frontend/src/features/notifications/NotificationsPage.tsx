import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Button, Center, Container, Divider, Group, Loader, Paper, SegmentedControl, Stack, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { IconChecks, IconSettings } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { client } from '../../api.ts';
import type { NotificationDto } from '../../api/generated.ts';
import { NOTIFICATION_SETTINGS_PATH, useNotifications } from './NotificationsContext.tsx';
import { NotificationItem } from './NotificationItem.tsx';

const PAGE_SIZE = 20;

type Filter = 'all' | 'unread';

export function NotificationsPage() {
    const { t } = useTranslation();
    useDocumentTitle(t('app.notifications.doc_title'));
    const { unreadCount, markAllRead, subscribe } = useNotifications();

    const [filter, setFilter] = useState<Filter>('all');
    const [items, setItems] = useState<NotificationDto[]>([]);
    const [nextCursor, setNextCursor] = useState<number | undefined>();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    const requestSeq = useRef(0);

    const load = useCallback(async (cursor?: number) => {
        const seq = ++requestSeq.current;
        setLoading(true);
        setError(false);
        try {
            const { data } = await client.api.listNotifications({
                cursor,
                size: PAGE_SIZE,
                unreadOnly: filter === 'unread',
            });
            if (seq !== requestSeq.current) return;
            setItems((current) => (cursor === undefined ? data.items : [...current, ...data.items]));
            setNextCursor(data.nextCursor ?? undefined);
        } catch {
            if (seq === requestSeq.current) setError(true);
        } finally {
            if (seq === requestSeq.current) setLoading(false);
        }
    }, [filter]);

    useEffect(() => {
        void load();
    }, [load]);

    // Mirror live arrivals and read-state changes made anywhere else (bell, toast, other tabs).
    useEffect(() => subscribe((change) => {
        setItems((current) => {
            switch (change.kind) {
                case 'created':
                    return current.some((n) => n.id === change.notification.id)
                        ? current
                        : [change.notification, ...current].sort((a, b) => b.id - a.id);
                case 'updated':
                    return current.map((n) => (n.id === change.notification.id ? change.notification : n));
                case 'allRead':
                    return current.map((n) => (n.id <= change.upToId && !n.readAt ? { ...n, readAt: change.readAt } : n));
            }
        });
    }), [subscribe]);

    return (
        <Container size="sm" py="xl">
            <Stack gap="md">
                <Group justify="space-between" align="center">
                    <Title order={2}>{t('app.notifications.title')}</Title>
                    <Group gap="xs">
                        <Button
                            variant="default"
                            leftSection={<IconChecks size={16} />}
                            disabled={unreadCount === 0}
                            onClick={() => void markAllRead()}
                        >
                            {t('app.notifications.mark_all_read')}
                        </Button>
                        <Button
                            variant="default"
                            leftSection={<IconSettings size={16} />}
                            component={Link}
                            to={NOTIFICATION_SETTINGS_PATH}
                        >
                            {t('app.notifications.settings.open')}
                        </Button>
                    </Group>
                </Group>

                <SegmentedControl
                    value={filter}
                    onChange={(value) => setFilter(value as Filter)}
                    data={[
                        { value: 'all', label: t('app.notifications.filter_all') },
                        { value: 'unread', label: `${t('app.notifications.filter_unread')}${unreadCount > 0 ? ` (${unreadCount})` : ''}` },
                    ]}
                    w="fit-content"
                />

                {error && <Alert color="red">{t('app.notifications.load_error')}</Alert>}

                <Paper withBorder radius="md" style={{ overflow: 'hidden' }}>
                    {items.length === 0 && !loading ? (
                        <Text c="dimmed" ta="center" py="xl">
                            {filter === 'unread' ? t('app.notifications.empty_unread') : t('app.notifications.empty')}
                        </Text>
                    ) : (
                        <Stack gap={0}>
                            {items.map((notification, index) => (
                                <div key={notification.id}>
                                    {index > 0 && <Divider />}
                                    <NotificationItem notification={notification} />
                                </div>
                            ))}
                        </Stack>
                    )}
                    {loading && (
                        <Center py="md">
                            <Loader size="sm" />
                        </Center>
                    )}
                </Paper>

                {nextCursor !== undefined && !loading && (
                    <Center>
                        <Button variant="default" onClick={() => void load(nextCursor)}>
                            {t('app.notifications.load_more')}
                        </Button>
                    </Center>
                )}
            </Stack>
        </Container>
    );
}
