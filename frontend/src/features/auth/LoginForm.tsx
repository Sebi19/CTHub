import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import {
    Alert,
    Anchor,
    Button,
    Group,
    Paper,
    PasswordInput,
    Stack,
    Text,
    TextInput,
} from '@mantine/core';
import { IconFingerprint, IconKey } from '@tabler/icons-react';
import { browserSupportsWebAuthnAutofill, WebAuthnAbortService } from '@simplewebauthn/browser';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import {
    AuthNextStep,
    LoginResultStatus,
    type LoginAttemptStateDto,
} from '../../api/generated.ts';
import type { TranslationKey } from '../../i18n/keys.ts';
import { getCurrentPushEndpoint } from '../notifications/push/pushSupport.ts';
import { apiErrorCode, apiErrorStatus } from './apiError.ts';
import { ChallengeWaiting } from './ChallengeWaiting.tsx';
import { getPasskeyAssertion, isPasskeyCancelled } from './passkey.ts';
import { useFinishSignIn } from './useFinishSignIn.ts';

type Step = 'email' | 'passkey' | 'password' | 'totp' | 'challenge';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * One entry point for signing in and signing up. The answer to "Continue" only says how to prove
 * the address - never whether an account exists: unknown addresses get the same e-mail/push
 * challenge as passwordless accounts, and the account is created once that challenge succeeds.
 */
export function LoginForm() {
    const { t, i18n } = useTranslation();
    const finishSignIn = useFinishSignIn();
    const [step, setStep] = useState<Step>('email');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [totp, setTotp] = useState('');
    const [passwordAvailable, setPasswordAvailable] = useState(false);
    const [attempt, setAttempt] = useState<LoginAttemptStateDto | null>(null);
    const [error, setError] = useState<TranslationKey | null>(null);
    const [busy, setBusy] = useState(false);
    const autoPasskeyStarted = useRef(false);

    const normalizedEmail = email.trim().toLowerCase();

    const reportError = useCallback((err: unknown) => {
        setError(apiErrorStatus(err) === 429 ? 'app.login.rate_limited' : 'app.login.error');
    }, []);

    // 1. Passkey autofill: offered by the browser in the e-mail field before anything is typed.
    useEffect(() => {
        if (step !== 'email') return;
        let active = true;
        void (async () => {
            if (!(await browserSupportsWebAuthnAutofill()) || !active) return;
            try {
                const { data: options } = await client.api.passkeyLoginOptions();
                if (!active) return;
                const credentialJson = await getPasskeyAssertion(options, true);
                const { data: user } = await client.api.loginWithPasskey({ credentialJson });
                finishSignIn(user, false);
            } catch (err) {
                if (active && !isPasskeyCancelled(err)) {
                    setError('app.login.passkey_failed');
                }
            }
        })();
        return () => {
            active = false;
            WebAuthnAbortService.cancelCeremony();
        };
    }, [step, finishSignIn]);

    const toChallenge = (next: LoginAttemptStateDto | undefined) => {
        if (!next) {
            setError('app.login.error');
            return;
        }
        setAttempt(next);
        setStep('challenge');
    };

    const submitEmail = async (event: FormEvent) => {
        event.preventDefault();
        if (!EMAIL_PATTERN.test(normalizedEmail)) {
            setError('app.login.email_invalid');
            return;
        }
        setError(null);
        setBusy(true);
        try {
            const pushEndpoint = await getCurrentPushEndpoint();
            const { data } = await client.api.identify({
                email: normalizedEmail,
                pushEndpoint,
                locale: i18n.resolvedLanguage,
            });
            setPasswordAvailable(!!data.passwordAvailable);
            if (data.next === AuthNextStep.PASSKEY) {
                autoPasskeyStarted.current = false;
                setStep('passkey');
            } else if (data.next === AuthNextStep.PASSWORD) {
                setStep('password');
            } else {
                toChallenge(data.attempt);
            }
        } catch (err) {
            reportError(err);
        } finally {
            setBusy(false);
        }
    };

    // 2. Account has passkeys: ask for one straight away.
    const signInWithPasskey = useCallback(async () => {
        setError(null);
        setBusy(true);
        try {
            const { data: options } = await client.api.passkeyLoginOptions();
            const credentialJson = await getPasskeyAssertion(options);
            const { data: user } = await client.api.loginWithPasskey({ credentialJson });
            finishSignIn(user, false);
        } catch (err) {
            if (!isPasskeyCancelled(err)) setError('app.login.passkey_failed');
        } finally {
            setBusy(false);
        }
    }, [finishSignIn]);

    useEffect(() => {
        if (step === 'passkey' && !autoPasskeyStarted.current) {
            autoPasskeyStarted.current = true;
            void signInWithPasskey();
        }
    }, [step, signInWithPasskey]);

    // 3. Optional password (+ authenticator code).
    const submitPassword = async (event: FormEvent) => {
        event.preventDefault();
        setError(null);
        setBusy(true);
        try {
            const { data } = await client.api.loginWithPassword({ email: normalizedEmail, password });
            if (data.status === LoginResultStatus.TOTP_REQUIRED) {
                setTotp('');
                setStep('totp');
            } else if (data.user) {
                finishSignIn(data.user, false);
            }
        } catch (err) {
            if (apiErrorCode(err) === 'INVALID_CREDENTIALS') {
                setError('app.login.invalid_credentials');
            } else {
                reportError(err);
            }
        } finally {
            setBusy(false);
        }
    };

    const submitTotp = async (event: FormEvent) => {
        event.preventDefault();
        setError(null);
        setBusy(true);
        try {
            const { data } = await client.api.loginWithPasswordAndTotp({ code: totp.replace(/\s/g, '') });
            if (data.user) finishSignIn(data.user, false);
        } catch (err) {
            const code = apiErrorCode(err);
            if (code === 'TOTP_SESSION_EXPIRED') {
                setPassword('');
                setStep('password');
                setError('app.login.totp_expired');
            } else if (code === 'TOTP_INVALID') {
                setError('app.login.totp_invalid');
            } else {
                reportError(err);
            }
        } finally {
            setBusy(false);
        }
    };

    // 4. Fallback for every other step: number matching via push and e-mail.
    const switchToChallenge = async () => {
        setError(null);
        setBusy(true);
        try {
            const pushEndpoint = await getCurrentPushEndpoint();
            const { data } = await client.api.startAttempt({
                email: normalizedEmail,
                pushEndpoint,
                locale: i18n.resolvedLanguage,
            });
            toChallenge(data);
        } catch (err) {
            reportError(err);
        } finally {
            setBusy(false);
        }
    };

    const restart = () => {
        setStep('email');
        setAttempt(null);
        setPassword('');
        setTotp('');
        setError(null);
    };

    const challengeFallback = (
        <Anchor component="button" type="button" size="sm" onClick={() => void switchToChallenge()}
                disabled={busy}>
            {t('app.login.use_challenge_instead')}
        </Anchor>
    );

    return (
        <Paper radius="md" p="xl" withBorder style={{ width: '100%', maxWidth: 420, margin: 'auto' }}>
            <Text size="lg" fw={500} mb={4}>{t('app.login.greeting')}</Text>
            {step === 'email' ? (
                <Text size="sm" c="dimmed" mb="md">{t('app.login.description')}</Text>
            ) : (
                <Group gap={6} mb="md" wrap="nowrap">
                    <Text size="sm" c="dimmed" truncate>{normalizedEmail}</Text>
                    {step !== 'challenge' && (
                        <Anchor component="button" type="button" size="sm" onClick={restart}>
                            {t('app.login.change_email')}
                        </Anchor>
                    )}
                </Group>
            )}

            {error && <Alert color="red" mb="md">{t(error)}</Alert>}

            {step === 'email' && (
                <form onSubmit={(e) => void submitEmail(e)} noValidate>
                    <Stack gap="md">
                        <TextInput
                            required
                            type="email"
                            name="username"
                            autoComplete="username webauthn"
                            autoFocus
                            label={t('app.login.email')}
                            placeholder={t('app.login.placeholder.email')}
                            value={email}
                            onChange={(e) => setEmail(e.currentTarget.value)}
                        />
                        <Button type="submit" radius="xl" loading={busy}>{t('app.login.continue')}</Button>
                    </Stack>
                </form>
            )}

            {step === 'passkey' && (
                <Stack gap="md">
                    <Text size="sm">{t('app.login.passkey_prompt')}</Text>
                    <Button radius="xl" leftSection={<IconFingerprint size={18} />} loading={busy}
                            onClick={() => void signInWithPasskey()}>
                        {t('app.login.passkey_button')}
                    </Button>
                    <Stack gap={4} align="center">
                        {passwordAvailable && (
                            <Anchor component="button" type="button" size="sm" onClick={() => {
                                setError(null);
                                setStep('password');
                            }}>
                                {t('app.login.use_password_instead')}
                            </Anchor>
                        )}
                        {challengeFallback}
                    </Stack>
                </Stack>
            )}

            {step === 'password' && (
                <form onSubmit={(e) => void submitPassword(e)}>
                    <Stack gap="md">
                        {/* Lets password managers file the credential under the right account. */}
                        <input type="email" name="username" autoComplete="username" value={normalizedEmail}
                               readOnly hidden />
                        <PasswordInput
                            required
                            autoFocus
                            autoComplete="current-password"
                            label={t('app.login.password')}
                            placeholder={t('app.login.placeholder.password')}
                            value={password}
                            onChange={(e) => setPassword(e.currentTarget.value)}
                        />
                        <Button type="submit" radius="xl" leftSection={<IconKey size={18} />} loading={busy}>
                            {t('app.login.login_button')}
                        </Button>
                        <Stack gap={4} align="center">{challengeFallback}</Stack>
                    </Stack>
                </form>
            )}

            {step === 'totp' && (
                <form onSubmit={(e) => void submitTotp(e)}>
                    <Stack gap="md">
                        <TextInput
                            required
                            autoFocus
                            inputMode="numeric"
                            autoComplete="one-time-code"
                            maxLength={8}
                            label={t('app.login.totp_label')}
                            description={t('app.login.totp_description')}
                            value={totp}
                            onChange={(e) => setTotp(e.currentTarget.value)}
                        />
                        <Button type="submit" radius="xl" loading={busy}>{t('app.login.verify_button')}</Button>
                        <Stack gap={4} align="center">{challengeFallback}</Stack>
                    </Stack>
                </form>
            )}

            {step === 'challenge' && attempt && (
                <ChallengeWaiting
                    attempt={attempt}
                    onCompleted={(completion) => finishSignIn(completion.user, true)}
                    onRestart={restart}
                />
            )}
        </Paper>
    );
}
