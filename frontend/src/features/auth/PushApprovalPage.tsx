import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Center, Container, Loader, Paper, Stack, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { client } from '../../api.ts';
import {
    ChallengePickResult,
    ChallengeViewKind,
    LoginAttemptPurpose,
    type ChallengeViewDto,
} from '../../api/generated.ts';
import { ChallengeNumberPicker } from './ChallengeNumberPicker.tsx';

type Outcome = 'approved' | 'rejected' | 'declined' | 'invalid';

/** Opened from the sign-in push on an already signed-in device. */
export function PushApprovalPage() {
    const { t } = useTranslation();
    useDocumentTitle(t('app.push_approval.doc_title'));
    const { attemptId = '' } = useParams();
    const [view, setView] = useState<ChallengeViewDto | null>(null);
    const [outcome, setOutcome] = useState<Outcome | null>(null);
    const [busy, setBusy] = useState(false);
    const started = useRef<string | null>(null);

    useEffect(() => {
        if (started.current === attemptId) return;
        started.current = attemptId;
        setView(null);
        setOutcome(null);
        void (async () => {
            try {
                const { data } = await client.api.viewPushApproval(attemptId);
                if (data.kind === ChallengeViewKind.INVALID) {
                    setOutcome('invalid');
                    return;
                }
                setView(data);
                await client.api.pushApprovalOpened(attemptId).catch(() => undefined);
            } catch {
                setOutcome('invalid');
            }
        })();
    }, [attemptId]);

    const pick = async (number: number) => {
        setBusy(true);
        try {
            const { data } = await client.api.approvePush(attemptId, { number });
            setOutcome(data.result === ChallengePickResult.APPROVED ? 'approved' : 'rejected');
        } catch {
            setOutcome('invalid');
        } finally {
            setBusy(false);
        }
    };

    const decline = async () => {
        setBusy(true);
        try {
            await client.api.rejectPush(attemptId);
        } catch { /* the attempt is gone either way */ }
        setOutcome('declined');
        setBusy(false);
    };

    let content;
    if (outcome) {
        const color = outcome === 'approved' ? 'green' : outcome === 'declined' ? 'blue' : 'red';
        content = (
            <Alert color={color} title={t(`app.auth_link.outcome.${outcome}.title` as const)}>
                {t(`app.auth_link.outcome.${outcome}.description` as const)}
            </Alert>
        );
    } else if (!view) {
        content = <Center><Loader /></Center>;
    } else {
        const isStepUp = view.purpose === LoginAttemptPurpose.STEP_UP;
        content = (
            <Stack gap="lg">
                <Stack gap={4}>
                    <Title order={3}>
                        {isStepUp ? t('app.auth_link.title_step_up') : t('app.auth_link.title_login')}
                    </Title>
                    <Text size="sm" c="dimmed">{t('app.push_approval.description')}</Text>
                </Stack>
                <ChallengeNumberPicker options={view.options} device={view.device} requestedAt={view.requestedAt}
                                       busy={busy} onPick={(n) => void pick(n)} />
                <Button variant="subtle" color="red" radius="xl" onClick={() => void decline()} disabled={busy}>
                    {t('app.auth_link.not_me')}
                </Button>
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
