import { Container, Title, Text, Button, Group, Stack } from '@mantine/core';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { IconHome } from '@tabler/icons-react';
import {useDocumentTitle} from "@mantine/hooks";

export function NotFoundPage() {
    const navigate = useNavigate();
    const { t } = useTranslation();

    useDocumentTitle(t('app.error.error_404.doc_title'));

    return (
        <Container size="sm" h="calc(100vh - 140px)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Stack align="center" gap="xl">
                <Title order={1} style={{ fontSize: '120px', lineHeight: 1, color: 'var(--mantine-color-blue-6)' }}>
                    404
                </Title>
                <Title order={2} ta="center">
                    {t('app.error.error_404.title')}
                </Title>
                <Text c="dimmed" ta="center" size="lg" maw={500}>
                    {t('app.error.error_404.description')}
                </Text>
                <Group justify="center" mt="md">
                    <Button size="md" variant="default" leftSection={<IconHome size={18} />} onClick={() => navigate('/')}>
                        {t('app.error.error_404.back_home')}
                    </Button>
                </Group>
            </Stack>
        </Container>
    );
}