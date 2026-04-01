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
} from '@mantine/core';
import {client} from '../../api.ts'
import {useNavigate} from "react-router-dom";
import {useState} from "react";
import {useAuth} from "./AuthContext.tsx"
import {type LoginRequestDto } from "../../api/generated.ts";
import {useTranslation} from "react-i18next";

export function LoginForm() {
    const { t} = useTranslation();
    const {login} = useAuth();
    const navigate = useNavigate();
    const [error, setError] = useState<string | null>(null);

    const form = useForm({
        initialValues: { email: '', password: '' },
        validate: {
            email: (val: string) => (val.length <= 2 ? t('app.login.email_too_short') : null),
            password: (val: string) => (val.length <= 2 ? t('app.login.password_too_short') : null),
        },
    });

    const handleSubmit = async (values: typeof form.values) => {
        setError(null);

        try {
            // 1. Create FormData (Spring Security expects form-urlencoded by default)
            const loginData: LoginRequestDto = {
                email: values.email,
                password: values.password
            };

            // 2. Perform the Login Request
            // We use standard fetch here because our generated client might enforce JSON
            const loginResponse = await client.api.login(loginData);

            if (!(loginResponse.status === 200)) {
                // FIX: Create an error that looks like an Axios error
                const error: any = new Error('Unbekannter Fehler');
                error.response = {
                    status: loginResponse.status,
                    statusText: loginResponse.statusText
                };
                throw error;
            }

            // 3. If successful, the Cookie is now set in the browser!
            // Now fetch the user profile to confirm who we are
            const userProfile = await client.api.getCurrentUser();

            // 4. Update Context
            login(userProfile.data);
            navigate('/');

        } catch (err: any) {
            // Axios error handling
            if (err.response?.status === 401 || err.response?.status === 403) {
                setError(t('app.login.invalid_credentials'));
            } else {
                setError(t('app.login.error') + ' ' + (err.message || 'Unbekannter Fehler'));
            }
        }
    };

    return (
        <Paper radius="md" p="xl" withBorder style={{ maxWidth: 400, margin: 'auto' }}>
            <Text size="lg" fw={500} mb="md">{t("app.login.greeting")}</Text>

            {error && <Alert color="red" mb="md">{error}</Alert>}

            <form onSubmit={form.onSubmit(handleSubmit)}>
                <Stack gap="md">
                    <TextInput
                        required
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
                </Group>
            </form>
        </Paper>
    );
}