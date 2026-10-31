import { useEffect, useRef, useState } from 'react';
import { Alert, Anchor, Center, Container, Loader, Paper, Stack } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router-dom';
import { client } from '../../api.ts';
import { useAuth } from '../auth/AuthContext.tsx';
import { accountErrorMessage } from './accountErrors.ts';

/** Target of the link sent to a new address; confirming is a POST so mail scanners cannot trigger it. */
export function ConfirmEmailPage() {
    const { t } = useTranslation();
    useDocumentTitle(t('app.account.email.confirm_doc_title'));
    const [params] = useSearchParams();
    const token = params.get('token') ?? '';
    const { isAuthenticated, login } = useAuth();
    const [state, setState] = useState<'pending' | 'done' | 'error'>('pending');
    const [error, setError] = useState<string | null>(null);
    const started = useRef(false);

    useEffect(() => {
        if (started.current) return;
        started.current = true;
        void (async () => {
            try {
                await client.api.confirmEmailChange({ token });
                setState('done');
                if (isAuthenticated) {
                    const { data } = await client.api.getCurrentUser().catch(() => ({ data: null }));
                    if (data) login(data);
                }
            } catch (err) {
                setError(accountErrorMessage(t, err));
                setState('error');
            }
        })();
    }, [token, isAuthenticated, login, t]);

    return (
        <Container size="xl" py="xl">
            <Center>
                <Paper radius="md" p="xl" withBorder style={{ width: '100%', maxWidth: 440 }}>
                    {state === 'pending' && <Center><Loader /></Center>}
                    {state !== 'pending' && (
                        <Stack gap="md">
                            {state === 'done' ? (
                                <Alert color="green" title={t('app.account.email.confirmed_title')}>
                                    {t('app.account.email.confirmed_description')}
                                </Alert>
                            ) : (
                                <Alert color="red" title={t('app.account.email.confirm_failed')}>{error}</Alert>
                            )}
                            <Anchor component={Link} to={isAuthenticated ? '/account' : '/login'} size="sm">
                                {isAuthenticated ? t('app.account.back_to_account') : t('app.auth_link.go_to_login')}
                            </Anchor>
                        </Stack>
                    )}
                </Paper>
            </Center>
        </Container>
    );
}
