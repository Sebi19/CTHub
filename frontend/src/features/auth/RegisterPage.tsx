import { Center, Container } from '@mantine/core';
import { RegisterForm } from './RegisterForm';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';

export function RegisterPage() {
    const { t } = useTranslation();
    useDocumentTitle(t('app.register.doc_title'));
    return (
        <Container size="xl" py="xl">
            <Center h="100%">
                <RegisterForm />
            </Center>
        </Container>
    );
}
