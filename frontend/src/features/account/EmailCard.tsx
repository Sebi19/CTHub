import { useState, type FormEvent } from 'react';
import { Alert, Button, Group, Paper, Stack, Text, TextInput, Title } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications as toasts } from '@mantine/notifications';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { client } from '../../api.ts';
import { useAuth } from '../auth/AuthContext.tsx';
import type { TranslationKey } from '../../i18n/keys.ts';
import { accountErrorKey } from './accountErrors.ts';

export function EmailCard() {
    const { t } = useTranslation();
    const { user } = useAuth();
    const [editing, setEditing] = useState(false);
    const [email, setEmail] = useState('');
    const [sentTo, setSentTo] = useState<string | null>(null);
    const [error, setError] = useState<TranslationKey | null>(null);
    const [busy, setBusy] = useState(false);

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        const newEmail = email.trim().toLowerCase();
        setError(null);
        setBusy(true);
        try {
            await client.api.requestEmailChange({ newEmail });
            setSentTo(newEmail);
            setEditing(false);
            setEmail('');
        } catch (err) {
            setError(accountErrorKey(err));
        } finally {
            setBusy(false);
        }
    };

    return (
        <Paper withBorder radius="md" p="lg">
            <Stack gap="md">
                <Group justify="space-between" align="flex-start">
                    <Stack gap={2} style={{ minWidth: 0 }}>
                        <Title order={4}>{t('app.account.email.title')}</Title>
                        <Text size="sm" truncate>{user?.email}</Text>
                    </Stack>
                    {!editing && (
                        <Button size="sm" radius="xl" variant="light" onClick={() => {
                            setSentTo(null);
                            setEditing(true);
                        }}>
                            {t('app.account.email.change')}
                        </Button>
                    )}
                </Group>
                {sentTo && <Alert color="green">{t('app.account.email.sent', { email: sentTo })}</Alert>}
                {editing && (
                    <form onSubmit={(e) => void submit(e)}>
                        <Stack gap="sm">
                            {error && <Alert color="red">{t(error)}</Alert>}
                            <TextInput required autoFocus type="email" autoComplete="email"
                                       label={t('app.account.email.new')}
                                       description={t('app.account.email.hint')}
                                       value={email} onChange={(e) => setEmail(e.currentTarget.value)} />
                            <Group justify="flex-end" gap="xs">
                                <Button variant="subtle" color="gray" radius="xl" disabled={busy}
                                        onClick={() => setEditing(false)}>
                                    {t('app.account.cancel')}
                                </Button>
                                <Button type="submit" radius="xl" loading={busy}>{t('app.account.email.send')}</Button>
                            </Group>
                        </Stack>
                    </form>
                )}
            </Stack>
        </Paper>
    );
}

export function DeleteAccountCard() {
    const { t } = useTranslation();
    const { logout } = useAuth();
    const navigate = useNavigate();

    const remove = () => modals.openConfirmModal({
        title: t('app.account.delete_account.title'),
        children: <Text size="sm">{t('app.account.delete_account.confirm')}</Text>,
        labels: { confirm: t('app.account.delete_account.button'), cancel: t('app.account.cancel') },
        confirmProps: { color: 'red' },
        onConfirm: async () => {
            try {
                await client.api.deleteAccount();
                await logout();
                toasts.show({ color: 'green', message: t('app.account.delete_account.done') });
                navigate('/', { replace: true });
            } catch (err) {
                toasts.show({ color: 'red', message: t(accountErrorKey(err)) });
            }
        },
    });

    return (
        <Paper withBorder radius="md" p="lg" style={{ borderColor: 'var(--mantine-color-red-6)' }}>
            <Group justify="space-between" align="flex-start">
                <Stack gap={2} style={{ flex: 1 }}>
                    <Title order={4}>{t('app.account.delete_account.title')}</Title>
                    <Text size="sm" c="dimmed">{t('app.account.delete_account.description')}</Text>
                </Stack>
                <Button size="sm" radius="xl" color="red" variant="light" onClick={remove}>
                    {t('app.account.delete_account.button')}
                </Button>
            </Group>
        </Paper>
    );
}
