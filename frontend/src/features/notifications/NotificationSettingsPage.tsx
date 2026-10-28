import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Anchor, Center, Container, Group, Loader, Paper, Stack, Switch, Table, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { notifications as toasts } from '@mantine/notifications';
import { IconArrowLeft } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { client } from '../../api.ts';
import type { NotificationChannelDto, NotificationPreferenceDto, NotificationTypeDto } from '../../api/generated.ts';
import { NOTIFICATIONS_PAGE_PATH } from './NotificationsContext.tsx';
import { getChannelLabel, getNotificationTypeLabel } from './notificationText.ts';
import { PushSettingsSection } from './push/PushSettingsSection.tsx';

const prefKey = (type: NotificationTypeDto, channel: NotificationChannelDto) => `${type}:${channel}`;

export function NotificationSettingsPage() {
    const { t } = useTranslation();
    useDocumentTitle(t('app.notifications.settings.doc_title'));

    const [preferences, setPreferences] = useState<NotificationPreferenceDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);
    const [saving, setSaving] = useState<Set<string>>(new Set());

    useEffect(() => {
        let cancelled = false;
        client.api.getNotificationPreferences()
            .then(({ data }) => { if (!cancelled) setPreferences(data); })
            .catch(() => { if (!cancelled) setError(true); })
            .finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };
    }, []);

    // Backend returns every configurable (type, channel) pair, so rows and columns are derived from it.
    const { types, channels, byKey } = useMemo(() => {
        const typeList: NotificationTypeDto[] = [];
        const channelList: NotificationChannelDto[] = [];
        const map = new Map<string, NotificationPreferenceDto>();
        for (const p of preferences) {
            if (!typeList.includes(p.type)) typeList.push(p.type);
            if (!channelList.includes(p.channel)) channelList.push(p.channel);
            map.set(prefKey(p.type, p.channel), p);
        }
        return { types: typeList, channels: channelList, byKey: map };
    }, [preferences]);

    const setEnabled = (type: NotificationTypeDto, channel: NotificationChannelDto, enabled: boolean) =>
        setPreferences((current) => current.map((p) => (p.type === type && p.channel === channel ? { ...p, enabled } : p)));

    const toggle = useCallback(async (type: NotificationTypeDto, channel: NotificationChannelDto, enabled: boolean) => {
        const key = prefKey(type, channel);
        setEnabled(type, channel, enabled);
        setSaving((s) => new Set(s).add(key));
        try {
            const { data } = await client.api.updateNotificationPreference({ type, channel, enabled });
            setEnabled(data.type, data.channel, data.enabled);
        } catch {
            setEnabled(type, channel, !enabled);
            toasts.show({ color: 'red', message: t('app.notifications.settings.save_error') });
        } finally {
            setSaving((s) => {
                const next = new Set(s);
                next.delete(key);
                return next;
            });
        }
    }, [t]);

    return (
        <Container size="sm" py="xl">
            <Stack gap="md">
                <Anchor component={Link} to={NOTIFICATIONS_PAGE_PATH} size="sm">
                    <Group gap={4}>
                        <IconArrowLeft size={14} />
                        {t('app.notifications.settings.back')}
                    </Group>
                </Anchor>
                <Title order={2}>{t('app.notifications.settings.title')}</Title>
                <Text c="dimmed">{t('app.notifications.settings.description')}</Text>

                {error && <Alert color="red">{t('app.notifications.settings.load_error')}</Alert>}

                {loading ? (
                    <Center py="xl"><Loader size="sm" /></Center>
                ) : !error && types.length === 0 ? (
                    <Text c="dimmed" ta="center" py="xl">{t('app.notifications.settings.empty')}</Text>
                ) : !error && (
                    <Paper withBorder radius="md" style={{ overflow: 'hidden' }}>
                        <Table verticalSpacing="md" horizontalSpacing="md">
                            <Table.Thead>
                                <Table.Tr>
                                    <Table.Th>{t('app.notifications.settings.type_column')}</Table.Th>
                                    {channels.map((channel) => (
                                        <Table.Th key={channel} ta="center" w={100}>{getChannelLabel(t, channel)}</Table.Th>
                                    ))}
                                </Table.Tr>
                            </Table.Thead>
                            <Table.Tbody>
                                {types.map((type) => {
                                    const { label, description } = getNotificationTypeLabel(t, type);
                                    return (
                                        <Table.Tr key={type}>
                                            <Table.Td>
                                                <Text fw={500}>{label}</Text>
                                                {description && <Text size="sm" c="dimmed">{description}</Text>}
                                            </Table.Td>
                                            {channels.map((channel) => {
                                                const pref = byKey.get(prefKey(type, channel));
                                                return (
                                                    <Table.Td key={channel}>
                                                        {pref && (
                                                            <Center>
                                                                <Switch
                                                                    checked={pref.enabled}
                                                                    disabled={saving.has(prefKey(type, channel))}
                                                                    aria-label={t('app.notifications.settings.toggle_label', {
                                                                        type: label,
                                                                        channel: getChannelLabel(t, channel),
                                                                    })}
                                                                    onChange={(e) => void toggle(type, channel, e.currentTarget.checked)}
                                                                />
                                                            </Center>
                                                        )}
                                                    </Table.Td>
                                                );
                                            })}
                                        </Table.Tr>
                                    );
                                })}
                            </Table.Tbody>
                        </Table>
                    </Paper>
                )}

                <PushSettingsSection />
            </Stack>
        </Container>
    );
}
