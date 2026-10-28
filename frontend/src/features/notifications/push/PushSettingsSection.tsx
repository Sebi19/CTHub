import { useCallback, useEffect, useState } from 'react';
import { ActionIcon, Alert, Badge, Button, Center, Divider, Group, Loader, Paper, Stack, Switch, Text, Title, Tooltip } from '@mantine/core';
import { notifications as toasts } from '@mantine/notifications';
import { IconDeviceMobile, IconSend, IconTrash } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { client } from '../../../api.ts';
import type { PushSubscriptionDto } from '../../../api/generated.ts';
import { formatNotificationTime } from '../notificationText.ts';
import { usePush } from './PushContext.tsx';
import { describeDevice, isIos, isStandalone } from './pushSupport.ts';

export function PushSettingsSection() {
    const { t } = useTranslation();
    const { supported, available, permission, subscribed, currentDeviceId, busy, enable, disable } = usePush();

    const [devices, setDevices] = useState<PushSubscriptionDto[]>([]);
    const [devicesLoading, setDevicesLoading] = useState(true);
    const [devicesError, setDevicesError] = useState(false);
    const [removing, setRemoving] = useState<number | null>(null);
    const [testing, setTesting] = useState(false);

    const loadDevices = useCallback(async () => {
        try {
            const { data } = await client.api.listPushSubscriptions();
            setDevices(data);
            setDevicesError(false);
        } catch {
            setDevicesError(true);
        } finally {
            setDevicesLoading(false);
        }
    }, []);

    // Reload whenever this device's state changes, so the list reflects enable/disable immediately.
    useEffect(() => {
        if (available) void loadDevices();
    }, [available, currentDeviceId, loadDevices]);

    const onToggle = async (checked: boolean) => {
        try {
            if (checked) await enable();
            else await disable();
        } catch {
            toasts.show({
                color: 'red',
                message: t(checked ? 'app.notifications.push.enable_error' : 'app.notifications.push.disable_error'),
            });
        }
    };

    const onTest = async () => {
        setTesting(true);
        try {
            const { data } = await client.api.sendTestPush();
            if (data.devices === 0) {
                toasts.show({ color: 'yellow', message: t('app.notifications.push.test_none') });
            } else {
                toasts.show({
                    color: data.delivered > 0 ? 'green' : 'red',
                    message: t('app.notifications.push.test_sent', { delivered: data.delivered, devices: data.devices }),
                });
            }
            // Devices the push service reported as gone were removed while sending.
            void loadDevices();
        } catch {
            toasts.show({ color: 'red', message: t('app.notifications.push.test_error') });
        } finally {
            setTesting(false);
        }
    };

    const onRemove = async (device: PushSubscriptionDto) => {
        setRemoving(device.id);
        try {
            if (device.id === currentDeviceId) await disable();
            else await client.api.deletePushSubscription(device.id);
            await loadDevices();
        } catch {
            toasts.show({ color: 'red', message: t('app.notifications.push.remove_error') });
        } finally {
            setRemoving(null);
        }
    };

    const renderControl = () => {
        if (!supported) {
            return isIos() && !isStandalone() ? (
                <Alert color="blue" title={t('app.notifications.push.ios_hint_title')}>
                    {t('app.notifications.push.ios_hint')}
                </Alert>
            ) : (
                <Text size="sm" c="dimmed">{t('app.notifications.push.unsupported')}</Text>
            );
        }
        if (available === null) return <Center><Loader size="sm" /></Center>;
        if (!available) return <Text size="sm" c="dimmed">{t('app.notifications.push.server_disabled')}</Text>;
        if (permission === 'denied') {
            return (
                <Alert color="orange" title={t('app.notifications.push.denied_title')}>
                    {t('app.notifications.push.denied')}
                </Alert>
            );
        }
        return (
            <Switch
                label={t('app.notifications.push.switch_label')}
                checked={subscribed}
                disabled={busy}
                onChange={(e) => void onToggle(e.currentTarget.checked)}
            />
        );
    };

    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="md">
                <div>
                    <Title order={4}>{t('app.notifications.push.title')}</Title>
                    <Text size="sm" c="dimmed">{t('app.notifications.push.description')}</Text>
                </div>

                {renderControl()}

                {available && (
                    <>
                        <Divider />
                        <Group justify="space-between">
                            <Text fw={500}>{t('app.notifications.push.devices_title')}</Text>
                            <Button
                                variant="light"
                                size="xs"
                                leftSection={<IconSend size={14} />}
                                loading={testing}
                                disabled={devices.length === 0}
                                onClick={() => void onTest()}
                            >
                                {t('app.notifications.push.test')}
                            </Button>
                        </Group>

                        {devicesLoading ? (
                            <Center><Loader size="sm" /></Center>
                        ) : devicesError ? (
                            <Alert color="red">{t('app.notifications.push.devices_load_error')}</Alert>
                        ) : devices.length === 0 ? (
                            <Text size="sm" c="dimmed">{t('app.notifications.push.devices_empty')}</Text>
                        ) : (
                            <Stack gap="xs">
                                {devices.map((device) => (
                                    <Group key={device.id} justify="space-between" wrap="nowrap">
                                        <Group gap="sm" wrap="nowrap">
                                            <IconDeviceMobile size={20} stroke={1.5} />
                                            <div>
                                                <Group gap={6}>
                                                    <Text size="sm" fw={500}>{describeDevice(t, device.userAgent)}</Text>
                                                    {device.id === currentDeviceId && (
                                                        <Badge size="xs" variant="light">{t('app.notifications.push.current_device')}</Badge>
                                                    )}
                                                    {device.installed && (
                                                        <Badge size="xs" variant="outline" color="gray">{t('app.notifications.push.installed')}</Badge>
                                                    )}
                                                </Group>
                                                <Text size="xs" c="dimmed">
                                                    {t('app.notifications.push.last_seen', { time: formatNotificationTime(device.lastSeenAt) })}
                                                </Text>
                                            </div>
                                        </Group>
                                        <Tooltip label={t('app.notifications.push.remove')}>
                                            <ActionIcon
                                                variant="subtle"
                                                color="red"
                                                aria-label={t('app.notifications.push.remove')}
                                                loading={removing === device.id}
                                                disabled={removing != null}
                                                onClick={() => void onRemove(device)}
                                            >
                                                <IconTrash size={16} />
                                            </ActionIcon>
                                        </Tooltip>
                                    </Group>
                                ))}
                            </Stack>
                        )}
                    </>
                )}
            </Stack>
        </Paper>
    );
}
