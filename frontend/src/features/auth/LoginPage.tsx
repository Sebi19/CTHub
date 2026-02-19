import { Center } from '@mantine/core';
import { LoginForm } from './LoginForm';
import { useDocumentTitle } from "@mantine/hooks";

export function LoginPage() {
    useDocumentTitle('Login | CTH')
    return (
            <Center h="100%">
                <LoginForm />
            </Center>
    );
}