import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Alert, Button, Loader, Modal, Stack, Text, TextInput } from '@mantine/core';
import { IconFingerprint, IconMail, IconShieldLock } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import type { LoginAttemptStateDto, StepUpMethodsDto } from '../../api/generated.ts';
import { getCurrentPushEndpoint } from '../notifications/push/pushSupport.ts';
import { apiErrorCode } from './apiError.ts';
import { ChallengeWaiting } from './ChallengeWaiting.tsx';
import { getPasskeyAssertion, isPasskeyCancelled } from './passkey.ts';
import { onStepUpRequired } from './stepUp.ts';

type Mode = 'choose' | 'totp' | 'challenge';

/**
 * Asks for a fresh proof of identity when the backend refuses a sensitive change with
 * `STEP_UP_REQUIRED`; the interceptor in `api.ts` then repeats the original request.
 */
export function StepUpModal() {
    const { t } = useTranslation();
    const resolver = useRef<((verified: boolean) => void) | null>(null);
    const [opened, setOpened] = useState(false);
    const [methods, setMethods] = useState<StepUpMethodsDto | null>(null);
    const [mode, setMode] = useState<Mode>('choose');
    const [attempt, setAttempt] = useState<LoginAttemptStateDto | null>(null);
    const [code, setCode] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        onStepUpRequired(() => new Promise<boolean>((resolve) => {
            resolver.current = resolve;
            setMode('choose');
            setAttempt(null);
            setCode('');
            setError(null);
            setMethods(null);
            setOpened(true);
            client.api.stepUpMethods()
                .then(({ data }) => setMethods(data))
                .catch(() => setMethods({}));
        }));
        return () => onStepUpRequired(null);
    }, []);

    const finish = (verified: boolean) => {
        resolver.current?.(verified);
        resolver.current = null;
        setOpened(false);
    };

    const close = () => {
        if (mode === 'challenge') {
            void client.api.cancelCurrentAttempt().catch(() => undefined);
        }
        finish(false);
    };

    const verifyWithPasskey = async () => {
        setError(null);
        setBusy(true);
        try {
            const { data: options } = await client.api.stepUpPasskeyOptions();
            const credentialJson = await getPasskeyAssertion(options);
            await client.api.stepUpWithPasskey({ credentialJson });
            finish(true);
        } catch (err) {
            if (!isPasskeyCancelled(err)) setError(t('app.step_up.passkey_failed'));
        } finally {
            setBusy(false);
        }
    };

    const submitTotp = async (event: FormEvent) => {
        event.preventDefault();
        setError(null);
        setBusy(true);
        try {
            await client.api.stepUpWithTotp({ code: code.replace(/\s/g, '') });
            finish(true);
        } catch (err) {
            setError(apiErrorCode(err) === 'TOTP_INVALID' ? t('app.step_up.totp_invalid') : t('app.step_up.error'));
        } finally {
            setBusy(false);
        }
    };

    const startChallenge = async () => {
        setError(null);
        setBusy(true);
        try {
            const pushEndpoint = await getCurrentPushEndpoint();
            const { data } = await client.api.startStepUpChallenge({ pushEndpoint });
            setAttempt(data);
            setMode('challenge');
        } catch {
            setError(t('app.step_up.error'));
        } finally {
            setBusy(false);
        }
    };

    return (
        <Modal opened={opened} onClose={close} title={t('app.step_up.title')} centered>
            <Stack gap="md">
                {mode !== 'challenge' && <Text size="sm" c="dimmed">{t('app.step_up.description')}</Text>}
                {error && <Alert color="red">{error}</Alert>}

                {mode === 'choose' && !methods && <Loader size="sm" mx="auto" />}

                {mode === 'choose' && methods && (
                    <Stack gap="xs">
                        {methods.passkey && (
                            <Button radius="xl" leftSection={<IconFingerprint size={18} />} loading={busy}
                                    onClick={() => void verifyWithPasskey()}>
                                {t('app.step_up.use_passkey')}
                            </Button>
                        )}
                        {methods.totp && (
                            <Button radius="xl" variant="light" leftSection={<IconShieldLock size={18} />}
                                    disabled={busy} onClick={() => {
                                        setError(null);
                                        setMode('totp');
                                    }}>
                                {t('app.step_up.use_totp')}
                            </Button>
                        )}
                        <Button radius="xl" variant={methods.passkey || methods.totp ? 'default' : 'filled'}
                                leftSection={<IconMail size={18} />} loading={busy && !methods.passkey}
                                disabled={busy} onClick={() => void startChallenge()}>
                            {t('app.step_up.use_challenge')}
                        </Button>
                    </Stack>
                )}

                {mode === 'totp' && (
                    <form onSubmit={(e) => void submitTotp(e)}>
                        <Stack gap="sm">
                            <TextInput required autoFocus inputMode="numeric" autoComplete="one-time-code"
                                       maxLength={8} label={t('app.step_up.totp_label')} value={code}
                                       onChange={(e) => setCode(e.currentTarget.value)} />
                            <Button type="submit" radius="xl" loading={busy}>{t('app.step_up.verify')}</Button>
                            <Button variant="subtle" color="gray" radius="xl" onClick={() => setMode('choose')}>
                                {t('app.step_up.back')}
                            </Button>
                        </Stack>
                    </form>
                )}

                {mode === 'challenge' && attempt && (
                    <ChallengeWaiting
                        attempt={attempt}
                        onCompleted={() => finish(true)}
                        onRestart={() => {
                            setAttempt(null);
                            setMode('choose');
                        }}
                    />
                )}
            </Stack>
        </Modal>
    );
}
