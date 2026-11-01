import { useEffect, useState } from 'react';
import { Affix, Button, Group, Paper, Stack, Text, ThemeIcon, Transition } from '@mantine/core';
import { notifications as toasts } from '@mantine/notifications';
import { IconDeviceMobile, IconDownload, IconShare2 } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import { useNotifications } from '../notifications/NotificationsContext.tsx';
import { usePush } from '../notifications/push/PushContext.tsx';
import { isStandalone } from '../notifications/push/pushSupport.ts';
import { PhoneQrModal } from './PhoneQrCode.tsx';
import {
    INSTALL_PARAM,
    isInstallBannerSnoozed,
    isMobileDevice,
    needsManualIosInstall,
    promptInstall,
    snoozeInstallBanner,
    useCanPromptInstall,
} from './pwaInstall.ts';

type Mode = 'prompt' | 'ios' | 'manual' | 'desktop';

const MOBILE_UA = /Android|iPhone|iPad|iPod|Mobi/i;

/**
 * Nudges towards the installed app, where push sign-in approvals work best:
 * one-tap install where the browser offers it, Share → "Add to Home Screen" steps on iOS, and on
 * desktop a QR code for signed-in users who have no phone receiving pushes yet.
 */
export function InstallBanner() {
    const { t } = useTranslation();
    const { enabled } = useNotifications();
    const { available } = usePush();
    const canPrompt = useCanPromptInstall();
    // Opened from the desktop QR code: the user asked for this, so ignore an earlier "not now".
    const [forced] = useState(() => new URLSearchParams(window.location.search).has(INSTALL_PARAM));
    const [dismissed, setDismissed] = useState(() => isInstallBannerSnoozed());
    const [phoneHasPush, setPhoneHasPush] = useState<boolean | null>(null);
    const [qrOpened, setQrOpened] = useState(false);

    const mobile = isMobileDevice();
    const standalone = isStandalone();
    const desktopCandidate = !mobile && !standalone && !dismissed && enabled && available === true;

    useEffect(() => {
        if (!desktopCandidate) return;
        let active = true;
        client.api.listPushSubscriptions()
            .then(({ data }) => {
                if (active) setPhoneHasPush(data.some((d) => d.installed || MOBILE_UA.test(d.userAgent ?? '')));
            })
            .catch(() => undefined);
        return () => {
            active = false;
        };
    }, [desktopCandidate]);

    let mode: Mode | null = null;
    if (mobile && !standalone && (forced || !dismissed)) {
        if (canPrompt) mode = 'prompt';
        else if (needsManualIosInstall()) mode = 'ios';
        else if (forced) mode = 'manual';
    } else if (desktopCandidate && phoneHasPush === false) {
        mode = 'desktop';
    }

    const later = () => {
        snoozeInstallBanner();
        setDismissed(true);
    };

    const install = async () => {
        if (await promptInstall()) toasts.show({ color: 'green', message: t('app.pwa.installed_toast') });
        setDismissed(true);
    };

    const body = mode === 'ios' ? t('app.pwa.banner.ios_steps')
        : mode === 'manual' ? t('app.pwa.banner.manual_steps')
            : mode === 'desktop' ? t('app.pwa.desktop.body')
                : t('app.pwa.banner.body');

    return (
        <>
            <Affix position={{ bottom: 16, left: 16, right: 16 }} zIndex={300}>
                <Transition mounted={mode != null && !qrOpened} transition="slide-up">
                    {(styles) => (
                        <Paper withBorder shadow="lg" radius="md" p="md" maw={480} mx="auto" style={styles}>
                            <Group align="flex-start" wrap="nowrap" gap="md">
                                <ThemeIcon variant="light" size="lg" radius="xl">
                                    {mode === 'ios' ? <IconShare2 size={18} />
                                        : mode === 'desktop' ? <IconDeviceMobile size={18} />
                                            : <IconDownload size={18} />}
                                </ThemeIcon>
                                <Stack gap="xs" style={{ flex: 1 }}>
                                    <Text fw={600}>
                                        {mode === 'desktop' ? t('app.pwa.desktop.title') : t('app.pwa.banner.title')}
                                    </Text>
                                    <Text size="sm" c="dimmed">{body}</Text>
                                    <Group gap="xs" justify="flex-end">
                                        <Button variant="subtle" color="gray" size="xs" onClick={later}>
                                            {t('app.pwa.later')}
                                        </Button>
                                        {mode === 'prompt' && (
                                            <Button size="xs" onClick={() => void install()}>
                                                {t('app.pwa.banner.install')}
                                            </Button>
                                        )}
                                        {mode === 'desktop' && (
                                            <Button size="xs" onClick={() => setQrOpened(true)}>
                                                {t('app.pwa.desktop.show_qr')}
                                            </Button>
                                        )}
                                    </Group>
                                </Stack>
                            </Group>
                        </Paper>
                    )}
                </Transition>
            </Affix>
            <PhoneQrModal opened={qrOpened} onClose={() => {
                setQrOpened(false);
                later();
            }} />
        </>
    );
}
