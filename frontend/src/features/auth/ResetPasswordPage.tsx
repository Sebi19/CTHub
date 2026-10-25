import { useForm } from '@mantine/form';
import { Container, Center, Paper, Title, Text, PasswordInput, Button, Group, Stack, Alert, Anchor } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useState } from 'react';
import { client } from '../../api.ts';
import { useAuth } from './AuthContext.tsx';
import { apiErrorCode, apiErrorMessage, apiErrorStatus } from './apiError.ts';

export function ResetPasswordPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { login } = useAuth();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const [error, setError] = useState<string | null>(null);
    const [tokenInvalid, setTokenInvalid] = useState(!token);
    const [submitting, setSubmitting] = useState(false);
    useDocumentTitle(t('app.reset_password.doc_title'));

    const form = useForm({
        initialValues: { password: '', confirmPassword: '' },
        validate: {
            password: (val: string) => (val.length < 8 ? t('app.reset_password.password_too_short') : null),
            confirmPassword: (val: string, values: { password: string }) =>
                val === values.password ? null : t('app.reset_password.passwords_do_not_match'),
        },
    });

    const handleSubmit = async (values: typeof form.values) => {
        if (!token) return;
        setError(null);
        setSubmitting(true);

        try {
            // A successful reset also signs the user in, so they land straight in the app.
            const response = await client.api.resetPassword({ token, password: values.password });
            login(response.data);
            navigate(response.data.profileComplete ? '/' : '/complete-profile', { replace: true });
        } catch (err: unknown) {
            if (apiErrorCode(err) === 'TOKEN_INVALID' || apiErrorStatus(err) === 400) {
                setTokenInvalid(true);
            } else {
                setError(t('app.reset_password.error') + ' ' + apiErrorMessage(err));
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <Paper radius="md" p="xl" withBorder style={{ maxWidth: 400, margin: 'auto' }}>
                    {tokenInvalid ? (
                        <Stack gap="md">
                            <Title order={3}>{t('app.reset_password.invalid_token_title')}</Title>
                            <Text c="dimmed">{t('app.reset_password.invalid_token_description')}</Text>
                            <Button radius="xl" onClick={() => navigate('/forgot-password')}>
                                {t('app.reset_password.request_new_link')}
                            </Button>
                        </Stack>
                    ) : (
                        <>
                            <Title order={3}>{t('app.reset_password.title')}</Title>
                            <Text size="sm" c="dimmed" mb="md">{t('app.reset_password.description')}</Text>

                            {error && <Alert color="red" mb="md">{error}</Alert>}

                            <form onSubmit={form.onSubmit(handleSubmit)}>
                                <Stack gap="md">
                                    <PasswordInput
                                        required
                                        label={t('app.reset_password.password')}
                                        placeholder={t('app.reset_password.placeholder_password')}
                                        {...form.getInputProps('password')}
                                    />
                                    <PasswordInput
                                        required
                                        label={t('app.reset_password.confirm_password')}
                                        placeholder={t('app.reset_password.placeholder_password')}
                                        {...form.getInputProps('confirmPassword')}
                                    />
                                </Stack>

                                <Group justify="space-between" mt="xl">
                                    <Button type="submit" radius="xl" loading={submitting}>
                                        {t('app.reset_password.submit_button')}
                                    </Button>
                                    <Anchor component={Link} to="/login" size="sm">
                                        {t('app.forgot_password.back_to_login')}
                                    </Anchor>
                                </Group>
                            </form>
                        </>
                    )}
                </Paper>
            </Center>
        </Container>
    );
}
