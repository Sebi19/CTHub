import { useEffect, useState } from 'react';
import { Affix, Anchor, Button, Group, Paper, Stack, Text, ThemeIcon, Transition } from '@mantine/core';
import { notifications as toasts } from '@mantine/notifications';
import { IconFingerprint } from '@tabler/icons-react';
import { platformAuthenticatorIsAvailable } from '@simplewebauthn/browser';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import { useAuth } from './AuthContext.tsx';
import { createPasskey, isPasskeyCancelled } from './passkey.ts';
import { clearPasskeyNudge, isPasskeyNudgeDue } from './passkeyNudge.ts';

/** After an e-mail/push sign-in on a device that could hold a passkey, offer one for next time. */
export function PasskeyNudgeBanner() {
    const { t } = useTranslation();
    const { user } = useAuth();
    const [visible, setVisible] = useState(false);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        if (!user?.profileComplete || !isPasskeyNudgeDue()) {
            setVisible(false);
            return;
        }
        let active = true;
        void platformAuthenticatorIsAvailable()
            .then((available) => {
                if (active) setVisible(available);
            })
            .catch(() => undefined);
        return () => {
            active = false;
        };
    }, [user]);

    const dismiss = (forever: boolean) => {
        clearPasskeyNudge(forever);
        setVisible(false);
    };

    const setUp = async () => {
        setBusy(true);
        try {
            const { data: options } = await client.api.passkeyRegistrationOptions();
            const credentialJson = await createPasskey(options);
            await client.api.registerPasskey({ credentialJson });
            toasts.show({ color: 'green', message: t('app.passkey_nudge.success') });
            dismiss(true);
        } catch (err) {
            if (!isPasskeyCancelled(err)) {
                toasts.show({ color: 'red', message: t('app.account.passkeys.add_error') });
            }
        } finally {
            setBusy(false);
        }
    };

    return (
        <Affix position={{ top: 76, left: 16, right: 16 }} zIndex={300}>
            <Transition mounted={visible} transition="slide-down">
                {(styles) => (
                    <Paper withBorder shadow="lg" radius="md" p="md" maw={480} mx="auto" style={styles}>
                        <Group align="flex-start" wrap="nowrap" gap="md">
                            <ThemeIcon variant="light" size="lg" radius="xl">
                                <IconFingerprint size={18} />
                            </ThemeIcon>
                            <Stack gap="xs" style={{ flex: 1 }}>
                                <Text fw={600}>{t('app.passkey_nudge.title')}</Text>
                                <Text size="sm" c="dimmed">{t('app.passkey_nudge.body')}</Text>
                                <Group gap="xs" justify="space-between">
                                    <Anchor component="button" type="button" size="xs" c="dimmed"
                                            onClick={() => dismiss(true)} disabled={busy}>
                                        {t('app.passkey_nudge.never')}
                                    </Anchor>
                                    <Group gap="xs">
                                        <Button variant="subtle" color="gray" size="xs" onClick={() => dismiss(false)}
                                                disabled={busy}>
                                            {t('app.passkey_nudge.later')}
                                        </Button>
                                        <Button size="xs" onClick={() => void setUp()} loading={busy}>
                                            {t('app.passkey_nudge.set_up')}
                                        </Button>
                                    </Group>
                                </Group>
                            </Stack>
                        </Group>
                    </Paper>
                )}
            </Transition>
        </Affix>
    );
}
