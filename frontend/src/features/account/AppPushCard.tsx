import { useEffect, useState } from 'react';
import { Anchor, Badge, Button, Divider, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { notifications as toasts } from '@mantine/notifications';
import { IconBellRinging, IconBrandChrome, IconDownload } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { client } from '../../api.ts';
import { NOTIFICATION_SETTINGS_PATH } from '../notifications/NotificationsContext.tsx';
import { usePush } from '../notifications/push/PushContext.tsx';
import { isIos, isStandalone } from '../notifications/push/pushSupport.ts';
import { PhoneQrCode } from '../pwa/PhoneQrCode.tsx';
import { chromeInstallIntentUrl, isMobileDevice, isSamsungInternet, promptInstall, useCanPromptInstall } from '../pwa/pwaInstall.ts';

/** Push on signed-in devices is a sign-in method: new sign-ins are approved there with one tap. */
export function AppPushCard() {
    const { t } = useTranslation();
    const { supported, available, permission, subscribed, currentDeviceId, busy, enable } = usePush();
    const canPrompt = useCanPromptInstall();
    const [deviceCount, setDeviceCount] = useState<number | null>(null);

    useEffect(() => {
        if (!available) return;
        let active = true;
        client.api.listPushSubscriptions()
            .then(({ data }) => {
                if (active) setDeviceCount(data.length);
            })
            .catch(() => undefined);
        return () => {
            active = false;
        };
    }, [available, currentDeviceId]);

    const onEnable = async () => {
        try {
            await enable();
        } catch {
            toasts.show({ color: 'red', message: t('app.notifications.push.enable_error') });
        }
    };

    const onInstall = async () => {
        if (await promptInstall()) toasts.show({ color: 'green', message: t('app.pwa.installed_toast') });
    };

    const renderThisDevice = () => {
        if (subscribed) return <Badge color="green" variant="light">{t('app.account.app.active')}</Badge>;
        if (!supported) {
            return (
                <Text size="sm" c="dimmed">
                    {isIos() && !isStandalone() ? t('app.account.app.ios_install') : t('app.notifications.push.unsupported')}
                </Text>
            );
        }
        if (available === false) return <Text size="sm" c="dimmed">{t('app.notifications.push.server_disabled')}</Text>;
        if (permission === 'denied') return <Text size="sm" c="dimmed">{t('app.account.app.denied')}</Text>;
        return (
            <Button size="sm" radius="xl" variant="light" leftSection={<IconBellRinging size={16} />}
                    loading={busy} disabled={available !== true} onClick={() => void onEnable()}>
                {t('app.account.app.enable')}
            </Button>
        );
    };

    const showQr = !isMobileDevice() && !isStandalone();

    return (
        <Paper withBorder radius="md" p="lg">
            <Stack gap="md">
                <Stack gap={2}>
                    <Title order={4}>{t('app.account.app.title')}</Title>
                    <Text size="sm" c="dimmed">{t('app.account.app.description')}</Text>
                </Stack>

                <Group justify="space-between" wrap="nowrap" gap="sm">
                    <Text fw={500}>{t('app.account.app.this_device')}</Text>
                    {renderThisDevice()}
                </Group>

                {available && deviceCount != null && (
                    <Group justify="space-between" wrap="nowrap" gap="sm">
                        <Text size="sm" c="dimmed">
                            {deviceCount === 0 ? t('app.account.app.devices_none')
                                : t('app.account.app.devices', { count: deviceCount })}
                        </Text>
                        <Anchor component={Link} to={NOTIFICATION_SETTINGS_PATH} size="sm" style={{ flexShrink: 0 }}>
                            {t('app.account.app.manage')}
                        </Anchor>
                    </Group>
                )}

                {canPrompt && (
                    <Group justify="space-between" wrap="nowrap" gap="sm">
                        <Text size="sm" c="dimmed">{t('app.account.app.install_description')}</Text>
                        <Button size="sm" radius="xl" variant="light" leftSection={<IconDownload size={16} />}
                                style={{ flexShrink: 0 }} onClick={() => void onInstall()}>
                            {t('app.pwa.banner.install')}
                        </Button>
                    </Group>
                )}

                {isSamsungInternet() && !isStandalone() && (
                    <Group justify="space-between" wrap="nowrap" gap="sm">
                        <Text size="sm" c="dimmed">{t('app.pwa.banner.chrome_body')}</Text>
                        <Button size="sm" radius="xl" variant="light" leftSection={<IconBrandChrome size={16} />}
                                style={{ flexShrink: 0 }} component="a" href={chromeInstallIntentUrl()}>
                            {t('app.pwa.banner.open_chrome')}
                        </Button>
                    </Group>
                )}

                {showQr && (
                    <>
                        <Divider />
                        <Group wrap="nowrap" align="center" gap="lg">
                            <PhoneQrCode size={120} />
                            <Stack gap={2}>
                                <Text fw={500}>{t('app.pwa.qr.title')}</Text>
                                <Text size="sm" c="dimmed">{t('app.pwa.qr.description')}</Text>
                            </Stack>
                        </Group>
                    </>
                )}
            </Stack>
        </Paper>
    );
}
