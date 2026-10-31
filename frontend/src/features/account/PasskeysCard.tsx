import { useState } from 'react';
import { ActionIcon, Badge, Button, Group, Paper, Stack, Text, TextInput, Title, Tooltip } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications as toasts } from '@mantine/notifications';
import { IconCheck, IconFingerprint, IconPencil, IconPlus, IconTrash, IconX } from '@tabler/icons-react';
import { browserSupportsWebAuthn } from '@simplewebauthn/browser';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import type { PasskeyDto } from '../../api/generated.ts';
import { createPasskey, isPasskeyCancelled } from '../auth/passkey.ts';
import { clearPasskeyNudge } from '../auth/passkeyNudge.ts';
import { accountErrorMessage } from './accountErrors.ts';

interface Props {
    passkeys: PasskeyDto[];
    onChange: () => void;
}

export function PasskeysCard({ passkeys, onChange }: Props) {
    const { t } = useTranslation();
    const [busy, setBusy] = useState(false);
    const [editing, setEditing] = useState<number | null>(null);
    const [name, setName] = useState('');
    const supported = browserSupportsWebAuthn();

    const add = async () => {
        setBusy(true);
        try {
            const { data: options } = await client.api.passkeyRegistrationOptions();
            const credentialJson = await createPasskey(options);
            await client.api.registerPasskey({ credentialJson });
            clearPasskeyNudge(true);
            toasts.show({ color: 'green', message: t('app.account.passkeys.added') });
            onChange();
        } catch (err) {
            if (!isPasskeyCancelled(err)) {
                toasts.show({ color: 'red', message: accountErrorMessage(t, err) });
            }
        } finally {
            setBusy(false);
        }
    };

    const saveName = async (id: number) => {
        const trimmed = name.trim();
        if (!trimmed) return;
        setBusy(true);
        try {
            await client.api.renamePasskey(id, { name: trimmed });
            setEditing(null);
            onChange();
        } catch (err) {
            toasts.show({ color: 'red', message: accountErrorMessage(t, err) });
        } finally {
            setBusy(false);
        }
    };

    const remove = (passkey: PasskeyDto) => modals.openConfirmModal({
        title: t('app.account.passkeys.delete_title'),
        children: <Text size="sm">{t('app.account.passkeys.delete_confirm', { name: passkey.name })}</Text>,
        labels: { confirm: t('app.account.delete'), cancel: t('app.account.cancel') },
        confirmProps: { color: 'red' },
        onConfirm: async () => {
            try {
                await client.api.deletePasskey(passkey.id);
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
                        <Title order={4}>{t('app.account.passkeys.title')}</Title>
                        <Text size="sm" c="dimmed">{t('app.account.passkeys.description')}</Text>
                    </Stack>
                    <Button leftSection={<IconPlus size={16} />} size="sm" radius="xl" onClick={() => void add()}
                            loading={busy && editing === null} disabled={!supported}>
                        {t('app.account.passkeys.add')}
                    </Button>
                </Group>
                {!supported && <Text size="sm" c="dimmed">{t('app.account.passkeys.unsupported')}</Text>}
                {passkeys.length === 0 && supported && (
                    <Text size="sm" c="dimmed">{t('app.account.passkeys.empty')}</Text>
                )}
                {passkeys.map((passkey) => (
                    <Group key={passkey.id} justify="space-between" wrap="nowrap" gap="sm">
                        <Group gap="sm" wrap="nowrap" style={{ minWidth: 0, flex: 1 }}>
                            <IconFingerprint size={22} style={{ flexShrink: 0 }} />
                            {editing === passkey.id ? (
                                <TextInput size="xs" value={name} maxLength={64} autoFocus style={{ flex: 1 }}
                                           onChange={(e) => setName(e.currentTarget.value)}
                                           onKeyDown={(e) => {
                                               if (e.key === 'Enter') void saveName(passkey.id);
                                               if (e.key === 'Escape') setEditing(null);
                                           }} />
                            ) : (
                                <Stack gap={0} style={{ minWidth: 0 }}>
                                    <Group gap={6} wrap="nowrap">
                                        <Text fw={500} truncate>{passkey.name}</Text>
                                        <Badge size="xs" variant="light" color={passkey.synced ? 'blue' : 'gray'}>
                                            {passkey.synced ? t('app.account.passkeys.synced')
                                                : t('app.account.passkeys.device_bound')}
                                        </Badge>
                                    </Group>
                                    <Text size="xs" c="dimmed">
                                        {t('app.account.passkeys.meta', {
                                            created: dayjs(passkey.createdAt).format('L'),
                                            used: passkey.lastUsedAt ? dayjs(passkey.lastUsedAt).format('L LT')
                                                : t('app.account.never'),
                                        })}
                                    </Text>
                                </Stack>
                            )}
                        </Group>
                        <Group gap={4} wrap="nowrap">
                            {editing === passkey.id ? (
                                <>
                                    <ActionIcon variant="subtle" onClick={() => void saveName(passkey.id)}
                                                loading={busy} aria-label={t('app.account.save')}>
                                        <IconCheck size={16} />
                                    </ActionIcon>
                                    <ActionIcon variant="subtle" color="gray" onClick={() => setEditing(null)}
                                                aria-label={t('app.account.cancel')}>
                                        <IconX size={16} />
                                    </ActionIcon>
                                </>
                            ) : (
                                <>
                                    <Tooltip label={t('app.account.passkeys.rename')}>
                                        <ActionIcon variant="subtle" color="gray" aria-label={t('app.account.passkeys.rename')}
                                                    onClick={() => {
                                                        setName(passkey.name);
                                                        setEditing(passkey.id);
                                                    }}>
                                            <IconPencil size={16} />
                                        </ActionIcon>
                                    </Tooltip>
                                    <Tooltip label={t('app.account.delete')}>
                                        <ActionIcon variant="subtle" color="red" aria-label={t('app.account.delete')}
                                                    onClick={() => remove(passkey)}>
                                            <IconTrash size={16} />
                                        </ActionIcon>
                                    </Tooltip>
                                </>
                            )}
                        </Group>
                    </Group>
                ))}
            </Stack>
        </Paper>
    );
}
