import { useForm } from '@mantine/form';
import {
    TextInput,
    PasswordInput,
    Text,
    Paper,
    Group,
    Button,
    Stack,
    Alert,
    Anchor,
    Divider,
} from '@mantine/core';
import { client } from '../../api.ts';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from './AuthContext.tsx';
import { type LoginRequestDto } from '../../api/generated.ts';
import { useTranslation } from 'react-i18next';
import { apiErrorCode, apiErrorMessage, apiErrorStatus } from './apiError.ts';

export function LoginForm() {
    const { t, i18n } = useTranslation();
    const { login } = useAuth();
    const navigate = useNavigate();
    const [error, setError] = useState<string | null>(null);
    const [accountNotVerified, setAccountNotVerified] = useState(false);
    const [noPasswordSet, setNoPasswordSet] = useState(false);
    const [loginLinkSent, setLoginLinkSent] = useState(false);
    const [sendingLoginLink, setSendingLoginLink] = useState(false);

    const form = useForm({
        initialValues: { email: '', password: '' },
        validate: {
            email: (val: string) => (val.length <= 2 ? t('app.login.email_too_short') : null),
            password: (val: string) => (val.length <= 2 ? t('app.login.password_too_short') : null),
        },
    });

    const resetFeedback = () => {
        setError(null);
        setAccountNotVerified(false);
        setNoPasswordSet(false);
        setLoginLinkSent(false);
    };

    const handleSubmit = async (values: typeof form.values) => {
        resetFeedback();

        try {
            const loginData: LoginRequestDto = {
                email: values.email.trim().toLowerCase(),
                password: values.password,
            };

            const loginResponse = await client.api.login(loginData);

            login(loginResponse.data);
            navigate(loginResponse.data.profileComplete ? '/' : '/complete-profile');
        } catch (err: unknown) {
            const code = apiErrorCode(err);
            const status = apiErrorStatus(err);
            if (code === 'ACCOUNT_NOT_VERIFIED') {
                setAccountNotVerified(true);
            } else if (code === 'NO_PASSWORD_SET') {
                setNoPasswordSet(true);
            } else if (status === 401 || status === 403) {
                setError(t('app.login.invalid_credentials'));
            } else {
                setError(t('app.login.error') + ' ' + apiErrorMessage(err));
            }
        }
    };

    const handleLoginLink = async () => {
        const email = form.values.email.trim().toLowerCase();
        if (email.length <= 2) {
            resetFeedback();
            setError(t('app.login.login_link_needs_email'));
            return;
        }

        resetFeedback();
        setSendingLoginLink(true);
        try {
            await client.api.requestLoginLink({ email, locale: i18n.resolvedLanguage });
        } catch {
            // Enumeration-safe endpoint (incl. rate limiting): always report the same outcome.
        } finally {
            setLoginLinkSent(true);
            setSendingLoginLink(false);
        }
    };

    return (
        <Paper radius="md" p="xl" withBorder style={{ maxWidth: 400, margin: 'auto' }}>
            <Text size="lg" fw={500} mb="md">{t('app.login.greeting')}</Text>

            {error && <Alert color="red" mb="md">{error}</Alert>}
            {loginLinkSent && <Alert color="green" mb="md">{t('app.login.login_link_sent')}</Alert>}
            {noPasswordSet && <Alert color="blue" mb="md">{t('app.login.no_password_set')}</Alert>}
            {accountNotVerified && (
                <Alert color="yellow" mb="md">
                    {t('app.login.account_not_verified')}{' '}
                    <Anchor component={Link} to="/register" size="sm">
                        {t('app.login.resend_verification_link')}
                    </Anchor>
                </Alert>
            )}

            <form onSubmit={form.onSubmit(handleSubmit)}>
                <Stack gap="md">
                    <TextInput
                        required
                        type="email"
                        label={t('app.login.email')}
                        placeholder={t('app.login.placeholder.email')}
                        {...form.getInputProps('email')}
                    />
                    <PasswordInput
                        required
                        label={t('app.login.password')}
                        placeholder={t('app.login.placeholder.password')}
                        {...form.getInputProps('password')}
                    />
                </Stack>

                <Group justify="space-between" mt="xl">
                    <Button type="submit" radius="xl">
                        {t('app.login.login_button')}
                    </Button>
                    <Anchor component={Link} to="/forgot-password" size="sm">
                        {t('app.login.forgot_password_link')}
                    </Anchor>
                </Group>
            </form>

            <Divider my="md" label={t('app.login.or_divider')} labelPosition="center" />

            <Button
                variant="light"
                radius="xl"
                fullWidth
                loading={sendingLoginLink}
                onClick={handleLoginLink}
            >
                {t('app.login.login_link_button')}
            </Button>

            <Text size="sm" mt="md" ta="center">
                {t('app.login.no_account_yet')}{' '}
                <Anchor component={Link} to="/register" size="sm">
                    {t('app.login.register_link')}
                </Anchor>
            </Text>
        </Paper>
    );
}
