import { Badge, Button, Group, Paper, Stack, Text, Title, ActionIcon, Tooltip } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications as toasts } from '@mantine/notifications';
import { IconDeviceDesktop, IconLogout } from '@tabler/icons-react';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import { AuthMethod, type SessionDto } from '../../api/generated.ts';
import { accountErrorKey } from './accountErrors.ts';

const METHOD_KEYS = {
    [AuthMethod.PASSKEY]: 'passkey',
    [AuthMethod.PASSWORD]: 'password',
    [AuthMethod.TOTP]: 'totp',
    [AuthMethod.EMAIL]: 'email',
    [AuthMethod.PUSH]: 'push',
} as const;

interface Props {
    sessions: SessionDto[];
    onChange: () => void;
}

export function SessionsCard({ sessions, onChange }: Props) {
    const { t } = useTranslation();
    const others = sessions.filter((s) => !s.current);

    const revoke = async (session: SessionDto) => {
        try {
            await client.api.revokeSession(session.id);
            onChange();
        } catch (err) {
            toasts.show({ color: 'red', message: t(accountErrorKey(err)) });
        }
    };

    const revokeOthers = () => modals.openConfirmModal({
        title: t('app.account.sessions.revoke_others_title'),
        children: <Text size="sm">{t('app.account.sessions.revoke_others_confirm')}</Text>,
        labels: { confirm: t('app.account.sessions.revoke_others'), cancel: t('app.account.cancel') },
        confirmProps: { color: 'red' },
        onConfirm: async () => {
            try {
                await client.api.revokeOtherSessions();
                onChange();
            } catch (err) {
                toasts.show({ color: 'red', message: t(accountErrorKey(err)) });
            }
        },
    });

    return (
        <Paper withBorder radius="md" p="lg">
            <Stack gap="md">
                <Group justify="space-between" align="flex-start">
                    <Stack gap={2}>
                        <Title order={4}>{t('app.account.sessions.title')}</Title>
                        <Text size="sm" c="dimmed">{t('app.account.sessions.description')}</Text>
                    </Stack>
                    {others.length > 0 && (
                        <Button size="sm" radius="xl" variant="subtle" color="red" onClick={revokeOthers}>
                            {t('app.account.sessions.revoke_others')}
                        </Button>
                    )}
                </Group>
                {sessions.map((session) => (
                    <Group key={session.id} justify="space-between" wrap="nowrap" gap="sm">
                        <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
                            <IconDeviceDesktop size={22} style={{ flexShrink: 0 }} />
                            <Stack gap={0} style={{ minWidth: 0 }}>
                                <Group gap={6} wrap="nowrap">
                                    <Text fw={500} truncate>{session.device ?? t('app.challenge.unknown_device')}</Text>
                                    {session.current && (
                                        <Badge size="xs" variant="light" color="green">
                                            {t('app.account.sessions.current')}
                                        </Badge>
                                    )}
                                    {session.installedApp && (
                                        <Badge size="xs" variant="light" color="blue">
                                            {t('app.account.sessions.installed_app')}
                                        </Badge>
                                    )}
                                </Group>
                                <Text size="xs" c="dimmed">
                                    {t('app.account.sessions.meta', {
                                        created: dayjs(session.createdAt).format('L'),
                                        used: dayjs(session.lastUsedAt).format('L LT'),
                                        methods: session.methods
                                            .map((m) => t(`app.account.sessions.method.${METHOD_KEYS[m]}` as const))
                                            .join(', '),
                                    })}
                                </Text>
                            </Stack>
                        </Group>
                        {!session.current && (
                            <Tooltip label={t('app.account.sessions.revoke')}>
                                <ActionIcon variant="subtle" color="red" aria-label={t('app.account.sessions.revoke')}
                                            onClick={() => void revoke(session)}>
                                    <IconLogout size={16} />
                                </ActionIcon>
                            </Tooltip>
                        )}
                    </Group>
                ))}
            </Stack>
        </Paper>
    );
}
