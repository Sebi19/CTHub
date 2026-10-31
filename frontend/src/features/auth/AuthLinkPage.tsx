import { useEffect, useRef, useState } from 'react';
import { Alert, Anchor, Button, Center, Container, Loader, Paper, Stack, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router-dom';
import { client } from '../../api.ts';
import {
    ChallengePickResult,
    ChallengeViewKind,
    LoginAttemptPurpose,
    type ChallengeViewDto,
} from '../../api/generated.ts';
import { apiErrorStatus } from './apiError.ts';
import { ChallengeNumberPicker } from './ChallengeNumberPicker.tsx';
import { useFinishSignIn } from './useFinishSignIn.ts';

type Outcome = 'approved' | 'rejected' | 'declined' | 'step_up_done' | 'invalid' | 'rate_limited';

/**
 * Landing page of the e-mailed link. Opening it never changes anything (mail scanners prefetch
 * links); every effect is a POST made by this page's script.
 */
export function AuthLinkPage() {
    const { t } = useTranslation();
    useDocumentTitle(t('app.auth_link.doc_title'));
    const [params] = useSearchParams();
    const token = params.get('token') ?? '';
    const finishSignIn = useFinishSignIn();
    const [view, setView] = useState<ChallengeViewDto | null>(null);
    const [outcome, setOutcome] = useState<Outcome | null>(null);
    const [busy, setBusy] = useState(false);
    const started = useRef(false);

    useEffect(() => {
        if (started.current) return;
        started.current = true;
        if (!token) {
            setOutcome('invalid');
            return;
        }
        void (async () => {
            try {
                const { data } = await client.api.viewLink({ token });
                if (data.kind === ChallengeViewKind.INVALID) {
                    setOutcome('invalid');
                    return;
                }
                if (data.kind === ChallengeViewKind.SAME_BROWSER) {
                    // The browser that asked for the link: no number needed.
                    const { data: completion } = await client.api.completeLink({ token });
                    if (completion.purpose === LoginAttemptPurpose.STEP_UP) {
                        setOutcome('step_up_done');
                    } else {
                        finishSignIn(completion.user, true);
                    }
                    return;
                }
                setView(data);
                await client.api.linkOpened({ token }).catch(() => undefined);
            } catch (err) {
                setOutcome(apiErrorStatus(err) === 429 ? 'rate_limited' : 'invalid');
            }
        })();
    }, [token, finishSignIn]);

    const pick = async (number: number) => {
        setBusy(true);
        try {
            const { data } = await client.api.approveLink({ token, number });
            setOutcome(data.result === ChallengePickResult.APPROVED ? 'approved' : 'rejected');
        } catch (err) {
            setOutcome(apiErrorStatus(err) === 429 ? 'rate_limited' : 'invalid');
        } finally {
            setBusy(false);
        }
    };

    const claimHere = async () => {
        setBusy(true);
        try {
            const { data: user } = await client.api.claimLinkHere({ token });
            finishSignIn(user, true);
        } catch (err) {
            setOutcome(apiErrorStatus(err) === 429 ? 'rate_limited' : 'invalid');
            setBusy(false);
        }
    };

    const decline = async () => {
        setBusy(true);
        try {
            await client.api.rejectLink({ token });
        } catch { /* the attempt is gone either way */ }
        setOutcome('declined');
        setBusy(false);
    };

    const isStepUp = view?.purpose === LoginAttemptPurpose.STEP_UP;

    let content;
    if (outcome) {
        const color = outcome === 'approved' || outcome === 'step_up_done' ? 'green'
            : outcome === 'declined' ? 'blue' : 'red';
        content = (
            <Stack gap="md">
                <Alert color={color} title={t(`app.auth_link.outcome.${outcome}.title` as const)}>
                    {t(`app.auth_link.outcome.${outcome}.description` as const)}
                </Alert>
                {(outcome === 'invalid' || outcome === 'rejected' || outcome === 'rate_limited') && (
                    <Anchor component={Link} to="/login" size="sm">{t('app.auth_link.go_to_login')}</Anchor>
                )}
            </Stack>
        );
    } else if (!view) {
        content = (
            <Stack align="center" gap="sm">
                <Loader />
                <Text size="sm" c="dimmed">{t('app.auth_link.loading')}</Text>
            </Stack>
        );
    } else {
        content = (
            <Stack gap="lg">
                <Stack gap={4}>
                    <Title order={3}>
                        {isStepUp ? t('app.auth_link.title_step_up') : t('app.auth_link.title_login')}
                    </Title>
                    <Text size="sm" c="dimmed">{t('app.auth_link.description')}</Text>
                </Stack>
                <ChallengeNumberPicker options={view.options} device={view.device} requestedAt={view.requestedAt}
                                       busy={busy} onPick={(n) => void pick(n)} />
                <Stack gap="xs">
                    {!isStepUp && (
                        <Button variant="light" radius="xl" onClick={() => void claimHere()} disabled={busy}>
                            {t('app.auth_link.claim_here')}
                        </Button>
                    )}
                    <Button variant="subtle" color="red" radius="xl" onClick={() => void decline()} disabled={busy}>
                        {t('app.auth_link.not_me')}
                    </Button>
                </Stack>
            </Stack>
        );
    }

    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <Paper radius="md" p="xl" withBorder style={{ width: '100%', maxWidth: 440, margin: 'auto' }}>
                    {content}
                </Paper>
            </Center>
        </Container>
    );
}
