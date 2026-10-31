import { useState, type FormEvent } from 'react';
import { Alert, Button, Group, Paper, PasswordInput, Stack, Text, Title } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications as toasts } from '@mantine/notifications';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import { useAuth } from '../auth/AuthContext.tsx';
import { accountErrorMessage } from './accountErrors.ts';

export const PASSWORD_MIN_LENGTH = 12;

interface Props {
    passwordSet: boolean;
    totpEnabled: boolean;
    onChange: () => void;
}

export function PasswordCard({ passwordSet, totpEnabled, onChange }: Props) {
    const { t } = useTranslation();
    const { login } = useAuth();
    const [editing, setEditing] = useState(false);
    const [password, setPassword] = useState('');
    const [confirm, setConfirm] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);

    const reset = () => {
        setEditing(false);
        setPassword('');
        setConfirm('');
        setError(null);
    };

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        if ([...password].length < PASSWORD_MIN_LENGTH) {
            setError(t('app.account.errors.password_too_short'));
            return;
        }
        if (password !== confirm) {
            setError(t('app.account.password.mismatch'));
            return;
        }
        setError(null);
        setBusy(true);
        try {
            const { data: user } = await client.api.setPassword({ password });
            login(user);
            toasts.show({ color: 'green', message: t('app.account.password.saved') });
            reset();
            onChange();
        } catch (err) {
            setError(accountErrorMessage(t, err));
        } finally {
            setBusy(false);
        }
    };

    const remove = () => modals.openConfirmModal({
        title: t('app.account.password.remove_title'),
        children: (
            <Text size="sm">
                {totpEnabled ? t('app.account.password.remove_confirm_totp') : t('app.account.password.remove_confirm')}
            </Text>
        ),
        labels: { confirm: t('app.account.password.remove'), cancel: t('app.account.cancel') },
        confirmProps: { color: 'red' },
        onConfirm: async () => {
            try {
                const { data: user } = await client.api.removePassword();
                login(user);
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
                    <Stack gap={2}>
                        <Title order={4}>{t('app.account.password.title')}</Title>
                        <Text size="sm" c="dimmed">
                            {passwordSet ? t('app.account.password.description_set')
                                : t('app.account.password.description_unset')}
                        </Text>
                    </Stack>
                    {!editing && (
                        <Group gap="xs">
                            {passwordSet && (
                                <Button size="sm" radius="xl" variant="subtle" color="red" onClick={remove}>
                                    {t('app.account.password.remove')}
                                </Button>
                            )}
                            <Button size="sm" radius="xl" variant="light" onClick={() => setEditing(true)}>
                                {passwordSet ? t('app.account.password.change') : t('app.account.password.set')}
                            </Button>
                        </Group>
                    )}
                </Group>

                {editing && (
                    <form onSubmit={(e) => void submit(e)}>
                        <Stack gap="sm">
                            {error && <Alert color="red">{error}</Alert>}
                            <PasswordInput required autoFocus autoComplete="new-password"
                                           label={t('app.account.password.new')}
                                           description={t('app.account.password.hint', { min: PASSWORD_MIN_LENGTH })}
                                           value={password} onChange={(e) => setPassword(e.currentTarget.value)} />
                            <PasswordInput required autoComplete="new-password"
                                           label={t('app.account.password.confirm')}
                                           value={confirm} onChange={(e) => setConfirm(e.currentTarget.value)} />
                            <Group justify="flex-end" gap="xs">
                                <Button variant="subtle" color="gray" radius="xl" onClick={reset} disabled={busy}>
                                    {t('app.account.cancel')}
                                </Button>
                                <Button type="submit" radius="xl" loading={busy}>{t('app.account.save')}</Button>
                            </Group>
                        </Stack>
                    </form>
                )}
            </Stack>
        </Paper>
    );
}
