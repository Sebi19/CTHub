import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Container, Center, Paper, Title, Text, Button, Stack, Loader, Group, Anchor, Alert } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Trans, useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import { RegistrationStatus } from '../../api/generated.ts';
import { useAuth } from './AuthContext.tsx';
import { clearPendingRegistration, readPendingRegistration } from './registrationHandle.ts';

const POLL_INTERVAL_MS = 3000;
const RESEND_COOLDOWN_SECONDS = 60;

type PageState = 'waiting' | 'awaiting_confirmation' | 'claimed_elsewhere' | 'cancelled' | 'unknown';

export function RegistrationPendingPage() {
    const { t, i18n } = useTranslation();
    const navigate = useNavigate();
    const { login } = useAuth();
    useDocumentTitle(t('app.registration_pending.doc_title'));

    const handle = useRef(readPendingRegistration()).current;

    const [state, setState] = useState<PageState>(handle ? 'waiting' : 'unknown');
    const [cooldown, setCooldown] = useState(RESEND_COOLDOWN_SECONDS);
    const [resent, setResent] = useState(false);
    const [resending, setResending] = useState(false);

    // Runs once polling reports VERIFIED, i.e. the number was picked correctly on the device that
    // holds the e-mailed link. Claiming is the one and only way this browser gains a session; the
    // poll endpoint itself never signs anyone in.
    const onVerified = useCallback(async () => {
        if (!handle) return;
        try {
            const me = await client.api.claimRegistration({ registrationId: handle.registrationId });
            clearPendingRegistration();
            login(me.data);
            navigate(me.data.profileComplete ? '/' : '/complete-profile', { replace: true });
        } catch {
            // The handle was spent or expired between the poll and the claim.
            clearPendingRegistration();
            setState('unknown');
        }
    }, [handle, login, navigate]);

    useEffect(() => {
        if (!handle || (state !== 'waiting' && state !== 'awaiting_confirmation')) return;

        let cancelled = false;

        const poll = async () => {
            try {
                const response = await client.api.registrationStatus({ registrationId: handle.registrationId });
                if (cancelled) return;

                switch (response.data.status) {
                    case RegistrationStatus.VERIFIED:
                        void onVerified();
                        break;
                    case RegistrationStatus.AWAITING_CONFIRMATION:
                        setState('awaiting_confirmation');
                        break;
                    case RegistrationStatus.CLAIMED_ELSEWHERE:
                        clearPendingRegistration();
                        setState('claimed_elsewhere');
                        break;
                    case RegistrationStatus.CANCELLED:
                        clearPendingRegistration();
                        setState('cancelled');
                        break;
                    case RegistrationStatus.UNKNOWN:
                        clearPendingRegistration();
                        setState('unknown');
                        break;
                    default:
                        break;
                }
            } catch {
                // Transient network/server hiccup: keep polling.
            }
        };

        void poll();
        const timer = setInterval(() => void poll(), POLL_INTERVAL_MS);
        return () => {
            cancelled = true;
            clearInterval(timer);
        };
    }, [handle, state, onVerified]);

    useEffect(() => {
        if (cooldown <= 0) return;
        const timer = setTimeout(() => setCooldown((value) => value - 1), 1000);
        return () => clearTimeout(timer);
    }, [cooldown]);

    const handleResend = async () => {
        if (!handle) return;
        setResending(true);
        try {
            await client.api.resendVerification({ email: handle.email, locale: i18n.resolvedLanguage });
        } catch {
            // Enumeration-safe endpoint: report success either way.
        } finally {
            setResent(true);
            setCooldown(RESEND_COOLDOWN_SECONDS);
            setResending(false);
        }
    };

    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <Paper radius="md" p="xl" withBorder style={{ maxWidth: 420, margin: 'auto' }}>
                    {(state === 'waiting' || state === 'awaiting_confirmation') && handle && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.registration_pending.title')}</Title>
                            <Text c="dimmed">
                                <Trans
                                    i18nKey="app.registration_pending.description"
                                    values={{ email: handle.email }}
                                    components={{ strong: <strong /> }}
                                />
                            </Text>

                            {handle.challengeNumber !== undefined && (
                                <Paper radius="md" p="md" withBorder bg="var(--mantine-color-default-hover)">
                                    <Stack gap={4} align="center">
                                        <Text size="sm" c="dimmed" ta="center">
                                            {t('app.registration_pending.challenge_hint')}
                                        </Text>
                                        <Text fz={48} fw={700} lh={1.1}>
                                            {handle.challengeNumber}
                                        </Text>
                                    </Stack>
                                </Paper>
                            )}

                            <Group gap="xs">
                                <Loader size="sm" />
                                <Text size="sm" c="dimmed">
                                    {state === 'awaiting_confirmation'
                                        ? t('app.registration_pending.awaiting_confirmation')
                                        : t('app.registration_pending.waiting')}
                                </Text>
                            </Group>

                            {resent && <Alert color="green">{t('app.registration_pending.resend_sent')}</Alert>}

                            <Button
                                variant="light"
                                radius="xl"
                                loading={resending}
                                disabled={cooldown > 0}
                                onClick={handleResend}
                            >
                                {cooldown > 0
                                    ? t('app.registration_pending.resend_available_in', { seconds: cooldown })
                                    : t('app.registration_pending.resend_button')}
                            </Button>

                            <Text size="sm" ta="center">
                                {t('app.registration_pending.wrong_email')}{' '}
                                <Anchor component={Link} to="/register" size="sm" onClick={clearPendingRegistration}>
                                    {t('app.registration_pending.start_over')}
                                </Anchor>
                            </Text>
                        </Stack>
                    )}

                    {state === 'claimed_elsewhere' && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.registration_pending.claimed_title')}</Title>
                            <Text c="dimmed">{t('app.registration_pending.claimed_description')}</Text>
                            <Button radius="xl" onClick={() => navigate('/login')}>
                                {t('app.registration_pending.go_to_login')}
                            </Button>
                        </Stack>
                    )}

                    {state === 'cancelled' && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.registration_pending.cancelled_title')}</Title>
                            <Text c="dimmed">{t('app.registration_pending.cancelled_description')}</Text>
                            <Button radius="xl" onClick={() => navigate('/register')}>
                                {t('app.registration_pending.start_over')}
                            </Button>
                            <Anchor component={Link} to="/login" size="sm" ta="center">
                                {t('app.registration_pending.back_to_login')}
                            </Anchor>
                        </Stack>
                    )}

                    {state === 'unknown' && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.registration_pending.unknown_title')}</Title>
                            <Text c="dimmed">{t('app.registration_pending.unknown_description')}</Text>
                            <Button radius="xl" onClick={() => navigate('/register')}>
                                {t('app.registration_pending.start_over')}
                            </Button>
                            <Anchor component={Link} to="/login" size="sm" ta="center">
                                {t('app.registration_pending.back_to_login')}
                            </Anchor>
                        </Stack>
                    )}
                </Paper>
            </Center>
        </Container>
    );
}
