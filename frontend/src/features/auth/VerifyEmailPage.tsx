import { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { Container, Center, Paper, Title, Text, Stack, Loader, Divider, Anchor, Button, Group, SimpleGrid } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Trans, useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import { ChallengeKind, type VerifyEmailRequestDto } from '../../api/generated.ts';
import { ResendVerificationForm } from './ResendVerificationForm.tsx';
import { useAuth } from './AuthContext.tsx';
import { clearPendingRegistration } from './registrationHandle.ts';

type VerificationState = 'loading' | 'challenge' | 'confirmed' | 'rejected' | 'mismatch' | 'error';

export function VerifyEmailPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { login } = useAuth();
    const [searchParams] = useSearchParams();
    const [state, setState] = useState<VerificationState>('loading');
    const [numbers, setNumbers] = useState<number[]>([]);
    const [email, setEmail] = useState<string | undefined>();
    const [busy, setBusy] = useState(false);
    const hasRun = useRef(false);

    useDocumentTitle(t('app.verify_email.doc_title'));

    const token = searchParams.get('token');

    // Signs this browser in. Used both when there is nothing to confirm and when the user explicitly
    // chooses to continue here instead - holding the e-mailed link is proof of inbox ownership
    // either way, so it is not a way around the challenge.
    const continueHere = useCallback(async () => {
        if (!token) return;
        setBusy(true);
        try {
            const data: VerifyEmailRequestDto = { token };
            const response = await client.api.verifyEmail(data);
            clearPendingRegistration();
            login(response.data);
            navigate(response.data.profileComplete ? '/' : '/complete-profile', { replace: true });
        } catch {
            setState('error');
        } finally {
            setBusy(false);
        }
    }, [token, login, navigate]);

    useEffect(() => {
        if (hasRun.current) return;
        hasRun.current = true;

        if (!token) {
            setState('error');
            return;
        }

        const load = async () => {
            try {
                const response = await client.api.verificationChallenge({ token });
                switch (response.data.kind) {
                    case ChallengeKind.CHALLENGE:
                        setNumbers(response.data.numbers ?? []);
                        setEmail(response.data.email);
                        setState('challenge');
                        break;
                    // Nobody is waiting elsewhere, or the waiting browser is this one: there is
                    // nothing to hand over, so skip the prompt entirely.
                    case ChallengeKind.SAME_BROWSER:
                    case ChallengeKind.NO_CHALLENGE:
                        void continueHere();
                        break;
                    default:
                        setState('error');
                }
            } catch {
                setState('error');
            }
        };

        void load();
    }, [token, continueHere]);

    const pick = async (number: number) => {
        if (!token) return;
        setBusy(true);
        try {
            await client.api.confirmChallenge({ token, number });
            setState('confirmed');
        } catch {
            // A wrong pick deactivates the link, so there is no second guess on this screen.
            setState('mismatch');
        } finally {
            setBusy(false);
        }
    };

    const reject = async () => {
        if (!token) return;
        setBusy(true);
        try {
            await client.api.rejectRegistration({ token });
        } catch {
            // Deliberately generic: the outcome shown does not depend on the link still being live.
        } finally {
            setBusy(false);
            setState('rejected');
        }
    };

    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <Paper radius="md" p="xl" withBorder style={{ maxWidth: 440, margin: 'auto' }}>
                    {state === 'loading' && (
                        <Stack align="center" gap="md">
                            <Loader />
                            <Text>{t('app.verify_email.verifying')}</Text>
                        </Stack>
                    )}

                    {state === 'challenge' && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.verify_email.challenge_title')}</Title>
                            <Text c="dimmed">
                                <Trans
                                    i18nKey="app.verify_email.challenge_description"
                                    values={{ email }}
                                    components={{ strong: <strong /> }}
                                />
                            </Text>

                            <SimpleGrid cols={3} spacing="sm">
                                {numbers.map((number) => (
                                    <Button
                                        key={number}
                                        variant="default"
                                        radius="md"
                                        size="lg"
                                        disabled={busy}
                                        onClick={() => void pick(number)}
                                    >
                                        {number}
                                    </Button>
                                ))}
                            </SimpleGrid>

                            <Divider my="xs" />

                            <Stack gap="xs">
                                <Button variant="light" radius="xl" loading={busy} onClick={() => void continueHere()}>
                                    {t('app.verify_email.continue_here')}
                                </Button>
                                <Button variant="subtle" color="red" radius="xl" disabled={busy} onClick={() => void reject()}>
                                    {t('app.verify_email.not_me')}
                                </Button>
                            </Stack>
                        </Stack>
                    )}

                    {state === 'confirmed' && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.verify_email.confirmed_title')}</Title>
                            <Text c="dimmed">{t('app.verify_email.confirmed_description')}</Text>
                        </Stack>
                    )}

                    {state === 'rejected' && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.verify_email.rejected_title')}</Title>
                            <Text c="dimmed">{t('app.verify_email.rejected_description')}</Text>
                            <Group justify="center">
                                <Anchor component={Link} to="/login" size="sm">
                                    {t('app.verify_email.go_to_login')}
                                </Anchor>
                            </Group>
                        </Stack>
                    )}

                    {state === 'mismatch' && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.verify_email.mismatch_title')}</Title>
                            <Text c="dimmed">{t('app.verify_email.mismatch_description')}</Text>
                            <Divider my="sm" />
                            <ResendVerificationForm />
                        </Stack>
                    )}

                    {state === 'error' && (
                        <Stack gap="md">
                            <Title order={3}>{t('app.verify_email.error_title')}</Title>
                            <Text c="dimmed">{t('app.verify_email.error_description')}</Text>
                            <Anchor component={Link} to="/login" size="sm">
                                {t('app.verify_email.go_to_login')}
                            </Anchor>
                            <Divider my="sm" />
                            <ResendVerificationForm />
                        </Stack>
                    )}
                </Paper>
            </Center>
        </Container>
    );
}
