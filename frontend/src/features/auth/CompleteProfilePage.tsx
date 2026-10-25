import { useForm } from '@mantine/form';
import { Container, Center, Paper, Title, Text, TextInput, PasswordInput, Button, Group, Stack, Alert } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { client } from '../../api.ts';
import { type CompleteProfileRequestDto } from '../../api/generated.ts';
import { useAuth } from './AuthContext.tsx';
import { apiErrorMessage } from './apiError.ts';

export function CompleteProfilePage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { user, login } = useAuth();
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);
    useDocumentTitle(t('app.complete_profile.doc_title'));

    const form = useForm({
        initialValues: {
            firstName: user?.firstName ?? '',
            lastName: user?.lastName ?? '',
            password: '',
        },
        validate: {
            firstName: (val: string) => (val.trim().length === 0 ? t('app.complete_profile.first_name_required') : null),
            lastName: (val: string) => (val.trim().length === 0 ? t('app.complete_profile.last_name_required') : null),
            // The password is optional; only validate it once something was typed.
            password: (val: string) => (val.length > 0 && val.length < 8 ? t('app.complete_profile.password_too_short') : null),
        },
    });

    const handleSubmit = async (values: typeof form.values) => {
        setError(null);
        setSubmitting(true);

        try {
            const data: CompleteProfileRequestDto = {
                firstName: values.firstName.trim(),
                lastName: values.lastName.trim(),
                password: values.password.length > 0 ? values.password : undefined,
            };

            const response = await client.api.completeProfile(data);
            login(response.data);
            navigate('/', { replace: true });
        } catch (err: unknown) {
            setError(t('app.complete_profile.error') + ' ' + apiErrorMessage(err));
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <Paper radius="md" p="xl" withBorder style={{ maxWidth: 420, margin: 'auto' }}>
                    <Title order={3}>{t('app.complete_profile.title')}</Title>
                    <Text size="sm" c="dimmed" mb="md">{t('app.complete_profile.description')}</Text>

                    {error && <Alert color="red" mb="md">{error}</Alert>}

                    <form onSubmit={form.onSubmit(handleSubmit)}>
                        <Stack gap="md">
                            <TextInput
                                required
                                label={t('app.complete_profile.first_name')}
                                placeholder={t('app.complete_profile.placeholder.first_name')}
                                {...form.getInputProps('firstName')}
                            />
                            <TextInput
                                required
                                label={t('app.complete_profile.last_name')}
                                placeholder={t('app.complete_profile.placeholder.last_name')}
                                {...form.getInputProps('lastName')}
                            />
                            <PasswordInput
                                label={t('app.complete_profile.password')}
                                placeholder={t('app.complete_profile.placeholder.password')}
                                description={t('app.complete_profile.password_hint')}
                                {...form.getInputProps('password')}
                            />
                        </Stack>

                        <Group justify="space-between" mt="xl">
                            <Button type="submit" radius="xl" loading={submitting}>
                                {t('app.complete_profile.submit_button')}
                            </Button>
                        </Group>
                    </form>
                </Paper>
            </Center>
        </Container>
    );
}
