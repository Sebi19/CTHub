import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Button, Group, Loader, Paper, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import {
    LoginAttemptState,
    type AttemptCompletionDto,
    type LoginAttemptStateDto,
} from '../../api/generated.ts';
import { apiErrorStatus } from './apiError.ts';

const POLL_MS = 2500;
const EMAIL_COOLDOWN_S = 30;

const FINAL_KEYS = {
    REJECTED: 'rejected',
    EXPIRED: 'expired',
    CANCELLED: 'cancelled',
    CLAIMED_ELSEWHERE: 'claimed_elsewhere',
    ERROR: 'error',
} as const;

type Final = keyof typeof FINAL_KEYS;

interface Props {
    attempt: LoginAttemptStateDto;
    onCompleted: (completion: AttemptCompletionDto) => void;
    /** Leave the waiting state (after cancelling or once the attempt is over). */
    onRestart: () => void;
}

/**
 * Browser A's half of the number-matching challenge: shows the number to pick on the other device
 * and polls until the attempt was approved, then completes it here.
 */
export function ChallengeWaiting({ attempt, onCompleted, onRestart }: Props) {
    const { t } = useTranslation();
    const [state, setState] = useState(attempt);
    const [final, setFinal] = useState<Final | null>(null);
    const [emailSent, setEmailSent] = useState(false);
    const [cooldown, setCooldown] = useState(0);
    const [busy, setBusy] = useState(false);
    const completing = useRef(false);
    const onCompletedRef = useRef(onCompleted);
    onCompletedRef.current = onCompleted;

    const complete = useCallback(async () => {
        if (completing.current) return;
        completing.current = true;
        try {
            const { data } = await client.api.completeCurrentAttempt();
            onCompletedRef.current(data);
        } catch {
            setFinal('ERROR');
        }
    }, []);

    /** The cookie is gone: another tab of this browser finished the attempt via the e-mailed link. */
    const adoptFinishedElsewhere = useCallback(async () => {
        try {
            const { data: user } = await client.api.getCurrentUser();
            onCompletedRef.current({ purpose: attempt.purpose, user });
        } catch {
            setFinal('EXPIRED');
        }
    }, [attempt.purpose]);

    useEffect(() => {
        if (final) return;
        let cancelled = false;
        let timer: number | undefined;

        const poll = async () => {
            try {
                const { data } = await client.api.getCurrentAttempt();
                if (cancelled) return;
                setState(data);
                switch (data.state) {
                    case LoginAttemptState.APPROVED:
                        await complete();
                        return;
                    case LoginAttemptState.COMPLETED:
                        await adoptFinishedElsewhere();
                        return;
                    case LoginAttemptState.WAITING:
                        break;
                    default:
                        setFinal(data.state as Final);
                        return;
                }
            } catch (err) {
                if (cancelled) return;
                if (apiErrorStatus(err) === 404) {
                    await adoptFinishedElsewhere();
                    return;
                }
                // Transient network trouble: keep polling.
            }
            if (!cancelled) timer = window.setTimeout(poll, POLL_MS);
        };

        timer = window.setTimeout(poll, POLL_MS);
        return () => {
            cancelled = true;
            window.clearTimeout(timer);
        };
    }, [final, complete, adoptFinishedElsewhere]);

    useEffect(() => {
        if (cooldown <= 0) return;
        const timer = window.setTimeout(() => setCooldown((c) => c - 1), 1000);
        return () => window.clearTimeout(timer);
    }, [cooldown]);

    const sendEmail = async () => {
        setBusy(true);
        try {
            await client.api.sendAttemptEmail();
            setEmailSent(true);
        } catch {
            // Rate limited or gone: the poll reports a dead attempt; otherwise just allow a retry later.
        } finally {
            setBusy(false);
            setCooldown(EMAIL_COOLDOWN_S);
        }
    };

    const cancel = async () => {
        setBusy(true);
        try {
            await client.api.cancelCurrentAttempt();
        } catch { /* already over */ }
        setBusy(false);
        onRestart();
    };

    if (final) {
        const key = FINAL_KEYS[final];
        return (
            <Stack gap="md">
                <Alert color={final === 'CLAIMED_ELSEWHERE' ? 'blue' : 'red'}
                       title={t(`app.challenge.final.${key}.title` as const)}>
                    {t(`app.challenge.final.${key}.description` as const)}
                </Alert>
                <Button radius="xl" onClick={onRestart}>{t('app.challenge.start_over')}</Button>
            </Stack>
        );
    }

    return (
        <Stack gap="md" align="stretch">
            <Text size="sm">{t('app.challenge.instructions')}</Text>
            <Paper withBorder radius="md" p="lg">
                <Stack gap={4} align="center">
                    <Text size="xs" c="dimmed" tt="uppercase">{t('app.challenge.your_number')}</Text>
                    <Title order={1} style={{ fontSize: 64, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}
                           aria-live="polite">
                        {String(state.challengeNumber ?? '--').padStart(2, '0')}
                    </Title>
                </Stack>
            </Paper>

            <Group gap="xs" justify="center">
                <Loader size="xs" />
                <Text size="sm" c="dimmed">
                    {state.openedElsewhere ? t('app.challenge.opened_elsewhere') : t('app.challenge.waiting')}
                </Text>
            </Group>

            {emailSent && <Alert color="green">{t('app.challenge.email_sent')}</Alert>}

            <Button variant="light" radius="xl" onClick={() => void sendEmail()} loading={busy}
                    disabled={cooldown > 0}>
                {cooldown > 0 ? t('app.challenge.email_cooldown', { seconds: cooldown })
                    : t('app.challenge.send_email')}
            </Button>
            <Button variant="subtle" color="gray" radius="xl" onClick={() => void cancel()} disabled={busy}>
                {t('app.challenge.cancel')}
            </Button>
        </Stack>
    );
}
