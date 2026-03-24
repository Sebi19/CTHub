import {Center, Container} from '@mantine/core';
import { LoginForm } from './LoginForm';
import { useDocumentTitle } from "@mantine/hooks";
import {useTranslation} from "react-i18next";

export function LoginPage() {
    const { t } = useTranslation();
    useDocumentTitle(t('app.login.doc_title'));
    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <LoginForm />
            </Center>
        </Container>
    );
}