import { useForm } from '@mantine/form';
import { TextInput, Text, Paper, Group, Button, Stack, Alert, Anchor } from '@mantine/core';
import { client } from '../../api.ts';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { type RegisterRequestDto, RegistrationStartStatus } from '../../api/generated.ts';
import { useTranslation } from 'react-i18next';
import { storePendingRegistration } from './registrationHandle.ts';
import { apiErrorMessage, apiErrorStatus } from './apiError.ts';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function RegisterForm() {
    const { t, i18n } = useTranslation();
    const navigate = useNavigate();
    const [error, setError] = useState<string | null>(null);
    const [alreadyRegistered, setAlreadyRegistered] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const form = useForm({
        initialValues: { email: '' },
        validate: {
            email: (val: string) => (EMAIL_PATTERN.test(val.trim()) ? null : t('app.register.email_invalid')),
        },
    });

    const handleSubmit = async (values: typeof form.values) => {
        setError(null);
        setAlreadyRegistered(false);
        setSubmitting(true);

        const email = values.email.trim().toLowerCase();

        try {
            const registerData: RegisterRequestDto = {
                email,
                // The explicitly selected UI language wins over the browser's Accept-Language header.
                locale: i18n.resolvedLanguage,
            };

            const response = await client.api.register(registerData);

            if (response.data.status === RegistrationStartStatus.ALREADY_REGISTERED || !response.data.registrationId) {
                setAlreadyRegistered(true);
                return;
            }

            storePendingRegistration({
                registrationId: response.data.registrationId,
                email,
                challengeNumber: response.data.challengeNumber,
            });
            navigate('/registration-pending');
        } catch (err: unknown) {
            if (apiErrorStatus(err) === 429) {
                setError(t('app.register.rate_limited'));
            } else {
                setError(t('app.register.error') + ' ' + apiErrorMessage(err));
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Paper radius="md" p="xl" withBorder style={{ maxWidth: 400, margin: 'auto' }}>
            <Text size="lg" fw={500}>{t('app.register.greeting')}</Text>
            <Text size="sm" c="dimmed" mb="md">{t('app.register.description')}</Text>

            {error && <Alert color="red" mb="md">{error}</Alert>}

            {alreadyRegistered && (
                <Alert color="yellow" mb="md">
                    <Stack gap={4}>
                        <Text size="sm">{t('app.register.already_registered')}</Text>
                        <Group gap="md">
                            <Anchor component={Link} to="/login" size="sm">
                                {t('app.register.already_registered_login')}
                            </Anchor>
                            <Anchor component={Link} to="/forgot-password" size="sm">
                                {t('app.register.already_registered_forgot')}
                            </Anchor>
                        </Group>
                    </Stack>
                </Alert>
            )}

            <form onSubmit={form.onSubmit(handleSubmit)}>
                <Stack gap="md">
                    <TextInput
                        required
                        type="email"
                        label={t('app.register.email')}
                        placeholder={t('app.register.placeholder.email')}
                        {...form.getInputProps('email')}
                    />
                </Stack>

                <Group justify="space-between" mt="xl">
                    <Button type="submit" radius="xl" loading={submitting}>
                        {t('app.register.register_button')}
                    </Button>
                </Group>
            </form>

            <Text size="sm" mt="md" ta="center">
                {t('app.register.already_have_account')}{' '}
                <Anchor component={Link} to="/login" size="sm">
                    {t('app.register.login_link')}
                </Anchor>
            </Text>
        </Paper>
    );
}
