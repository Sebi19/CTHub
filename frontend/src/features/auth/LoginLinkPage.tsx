import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Container, Center, Paper, Title, Text, Button, Stack, Loader, Anchor } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import { useAuth } from './AuthContext.tsx';

export function LoginLinkPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { login } = useAuth();
    const [searchParams] = useSearchParams();
    const [failed, setFailed] = useState(false);
    const hasRun = useRef(false);

    useDocumentTitle(t('app.login_link.doc_title'));

    useEffect(() => {
        if (hasRun.current) return;
        hasRun.current = true;

        const token = searchParams.get('token');
        if (!token) {
            setFailed(true);
            return;
        }

        const consume = async () => {
            try {
                const response = await client.api.consumeLoginLink({ token });
                login(response.data);
                navigate(response.data.profileComplete ? '/' : '/complete-profile', { replace: true });
            } catch {
                setFailed(true);
            }
        };

        void consume();
    }, [searchParams, login, navigate]);

    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <Paper radius="md" p="xl" withBorder style={{ maxWidth: 400, margin: 'auto' }}>
                    {failed ? (
                        <Stack gap="md">
                            <Title order={3}>{t('app.login_link.error_title')}</Title>
                            <Text c="dimmed">{t('app.login_link.error_description')}</Text>
                            <Button radius="xl" onClick={() => navigate('/login')}>
                                {t('app.login_link.go_to_login')}
                            </Button>
                            <Anchor component={Link} to="/forgot-password" size="sm" ta="center">
                                {t('app.login.forgot_password_link')}
                            </Anchor>
                        </Stack>
                    ) : (
                        <Stack align="center" gap="md">
                            <Loader />
                            <Text>{t('app.login_link.consuming')}</Text>
                        </Stack>
                    )}
                </Paper>
            </Center>
        </Container>
    );
}
