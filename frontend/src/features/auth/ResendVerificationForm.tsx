import { useForm } from '@mantine/form';
import { TextInput, Button, Group, Stack, Alert, Text } from '@mantine/core';
import { client } from '../../api.ts';
import { useState } from 'react';
import { type ResendVerificationRequestDto } from '../../api/generated.ts';
import { useTranslation } from 'react-i18next';
import { apiErrorMessage, apiErrorStatus } from './apiError.ts';

export function ResendVerificationForm() {
    const { t, i18n } = useTranslation();
    const [sent, setSent] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    const form = useForm({
        initialValues: { email: '' },
        validate: {
            email: (val: string) => (val.length <= 2 ? t('app.login.email_too_short') : null),
        },
    });

    const handleSubmit = async (values: typeof form.values) => {
        setError(null);
        setSubmitting(true);

        try {
            const data: ResendVerificationRequestDto = {
                email: values.email.trim().toLowerCase(),
                locale: i18n.resolvedLanguage,
            };
            await client.api.resendVerification(data);
            setSent(true);
        } catch (err: unknown) {
            if (apiErrorStatus(err) === 429) {
                // Rate limited: still show the generic sent-message to avoid enumeration.
                setSent(true);
            } else {
                setError(t('app.register.error') + ' ' + apiErrorMessage(err));
            }
        } finally {
            setSubmitting(false);
        }
    };

    if (sent) {
        return <Text size="sm" c="dimmed">{t('app.resend_verification.sent_message')}</Text>;
    }

    return (
        <form onSubmit={form.onSubmit(handleSubmit)}>
            <Text size="sm" fw={500} mb="xs">{t('app.resend_verification.title')}</Text>

            {error && <Alert color="red" mb="md">{error}</Alert>}

            <Stack gap="sm">
                <TextInput
                    required
                    label={t('app.resend_verification.email')}
                    placeholder={t('app.resend_verification.placeholder_email')}
                    {...form.getInputProps('email')}
                />
            </Stack>

            <Group justify="flex-start" mt="md">
                <Button type="submit" radius="xl" size="sm" loading={submitting}>
                    {t('app.resend_verification.submit_button')}
                </Button>
            </Group>
        </form>
    );
}
