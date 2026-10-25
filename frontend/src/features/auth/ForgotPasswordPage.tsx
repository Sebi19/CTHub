import { useForm } from '@mantine/form';
import { Container, Center, Paper, Title, Text, TextInput, Button, Group, Stack, Alert, Anchor } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { client } from '../../api.ts';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function ForgotPasswordPage() {
    const { t, i18n } = useTranslation();
    const [sent, setSent] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    useDocumentTitle(t('app.forgot_password.doc_title'));

    const form = useForm({
        initialValues: { email: '' },
        validate: {
            email: (val: string) => (EMAIL_PATTERN.test(val.trim()) ? null : t('app.forgot_password.email_invalid')),
        },
    });

    const handleSubmit = async (values: typeof form.values) => {
        setSubmitting(true);
        try {
            await client.api.forgotPassword({
                email: values.email.trim().toLowerCase(),
                locale: i18n.resolvedLanguage,
            });
        } catch {
            // Enumeration-safe endpoint (incl. rate limiting): always report the same outcome.
        } finally {
            setSent(true);
            setSubmitting(false);
        }
    };

    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <Paper radius="md" p="xl" withBorder style={{ maxWidth: 400, margin: 'auto' }}>
                    <Title order={3}>{t('app.forgot_password.title')}</Title>

                    {sent ? (
                        <Stack gap="md" mt="md">
                            <Alert color="green">{t('app.forgot_password.sent_message')}</Alert>
                            <Anchor component={Link} to="/login" size="sm" ta="center">
                                {t('app.forgot_password.back_to_login')}
                            </Anchor>
                        </Stack>
                    ) : (
                        <>
                            <Text size="sm" c="dimmed" mb="md">{t('app.forgot_password.description')}</Text>

                            <form onSubmit={form.onSubmit(handleSubmit)}>
                                <Stack gap="md">
                                    <TextInput
                                        required
                                        type="email"
                                        label={t('app.forgot_password.email')}
                                        placeholder={t('app.forgot_password.placeholder_email')}
                                        {...form.getInputProps('email')}
                                    />
                                </Stack>

                                <Group justify="space-between" mt="xl">
                                    <Button type="submit" radius="xl" loading={submitting}>
                                        {t('app.forgot_password.submit_button')}
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
