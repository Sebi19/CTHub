import { useEffect, useState, type FormEvent } from 'react';
import { Alert, Badge, Button, Code, Group, Image, Paper, Stack, Text, TextInput, Title } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications as toasts } from '@mantine/notifications';
import QRCode from 'qrcode';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import type { TotpEnrollmentDto } from '../../api/generated.ts';
import { accountErrorMessage } from './accountErrors.ts';

interface Props {
    enabled: boolean;
    available: boolean;
    passwordSet: boolean;
    onChange: () => void;
}

/** Authenticator app: only a second factor to the password (and a step-up method), never a login on its own. */
export function TotpCard({ enabled, available, passwordSet, onChange }: Props) {
    const { t } = useTranslation();
    const [enrollment, setEnrollment] = useState<TotpEnrollmentDto | null>(null);
    const [qr, setQr] = useState<string | null>(null);
    const [code, setCode] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        if (!enrollment) {
            setQr(null);
            return;
        }
        let active = true;
        QRCode.toDataURL(enrollment.otpauthUri, { margin: 1, width: 200 })
            .then((url) => {
                if (active) setQr(url);
            })
            .catch(() => undefined);
        return () => {
            active = false;
        };
    }, [enrollment]);

    const begin = async () => {
        setError(null);
        setBusy(true);
        try {
            const { data } = await client.api.beginTotp();
            setCode('');
            setEnrollment(data);
        } catch (err) {
            toasts.show({ color: 'red', message: accountErrorMessage(t, err) });
        } finally {
            setBusy(false);
        }
    };

    const confirm = async (event: FormEvent) => {
        event.preventDefault();
        setError(null);
        setBusy(true);
        try {
            await client.api.confirmTotp({ code: code.replace(/\s/g, '') });
            setEnrollment(null);
            toasts.show({ color: 'green', message: t('app.account.totp.enabled_toast') });
            onChange();
        } catch (err) {
            setError(accountErrorMessage(t, err));
        } finally {
            setBusy(false);
        }
    };

    const disable = () => modals.openConfirmModal({
        title: t('app.account.totp.disable_title'),
        children: <Text size="sm">{t('app.account.totp.disable_confirm')}</Text>,
        labels: { confirm: t('app.account.totp.disable'), cancel: t('app.account.cancel') },
        confirmProps: { color: 'red' },
        onConfirm: async () => {
            try {
                await client.api.disableTotp();
                onChange();
            } catch (err) {
                toasts.show({ color: 'red', message: accountErrorMessage(t, err) });
            }
        },
    });

    return (
        <Paper withBorder radius="md" p="lg">
            <Stack gap="md">
                <Group justify="space-between" align="flex-start">
                    <Stack gap={2} style={{ flex: 1 }}>
                        <Group gap="xs">
                            <Title order={4}>{t('app.account.totp.title')}</Title>
                            {enabled && <Badge color="green" variant="light">{t('app.account.totp.active')}</Badge>}
                        </Group>
                        <Text size="sm" c="dimmed">{t('app.account.totp.description')}</Text>
                    </Stack>
                    {!enrollment && available && (enabled ? (
                        <Button size="sm" radius="xl" variant="subtle" color="red" onClick={disable}>
                            {t('app.account.totp.disable')}
                        </Button>
                    ) : (
                        <Button size="sm" radius="xl" variant="light" onClick={() => void begin()} loading={busy}
                                disabled={!passwordSet}>
                            {t('app.account.totp.set_up')}
                        </Button>
                    ))}
                </Group>

                {!available && <Text size="sm" c="dimmed">{t('app.account.errors.totp_unavailable')}</Text>}
                {available && !enabled && !passwordSet && (
                    <Text size="sm" c="dimmed">{t('app.account.totp.needs_password')}</Text>
                )}

                {enrollment && (
                    <form onSubmit={(e) => void confirm(e)}>
                        <Stack gap="sm">
                            <Text size="sm">{t('app.account.totp.scan')}</Text>
                            {qr && <Image src={qr} alt={t('app.account.totp.qr_alt')} w={200} h={200} mx="auto" />}
                            <Text size="xs" c="dimmed">{t('app.account.totp.manual')}</Text>
                            <Code block style={{ wordBreak: 'break-all' }}>{enrollment.secret}</Code>
                            {error && <Alert color="red">{error}</Alert>}
                            <TextInput required inputMode="numeric" autoComplete="one-time-code" maxLength={8}
                                       label={t('app.account.totp.code')} value={code}
                                       onChange={(e) => setCode(e.currentTarget.value)} />
                            <Group justify="flex-end" gap="xs">
                                <Button variant="subtle" color="gray" radius="xl" disabled={busy}
                                        onClick={() => setEnrollment(null)}>
                                    {t('app.account.cancel')}
                                </Button>
                                <Button type="submit" radius="xl" loading={busy}>{t('app.account.totp.activate')}</Button>
                            </Group>
                        </Stack>
                    </form>
                )}
            </Stack>
        </Paper>
    );
}
