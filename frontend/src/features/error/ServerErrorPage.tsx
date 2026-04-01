import { Container, Title, Text, Button, Group, Stack } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { IconRefresh } from '@tabler/icons-react';
import {useDocumentTitle} from "@mantine/hooks";
import {NavigateBackButton} from "../common/navigation/NavigateBackButton.tsx";

export function ServerErrorPage({handleBackNavigation}: {handleBackNavigation?: () => void}) {
    const { t } = useTranslation();

    useDocumentTitle(t('app.error.error_500.doc_title'));

    return (
        <Container size="xl" py="xl">
            {handleBackNavigation && (
                <NavigateBackButton handleBackNavigation={handleBackNavigation} />
            )}
            <Stack align="center" gap="xl">
                <Title order={1} style={{ fontSize: '120px', lineHeight: 1, color: 'var(--mantine-color-red-6)' }}>
                    500
                </Title>
                <Title order={2} ta="center">
                    {t('app.error.error_500.title')}
                </Title>
                <Text c="dimmed" ta="center" size="lg" maw={500}>
                    {t('app.error.error_500.description')}
                </Text>
                <Group justify="center" mt="md">
                    <Button size="md" variant="default" leftSection={<IconRefresh size={18} />} onClick={() => window.location.reload()}>
                        {t('app.error.error_500.retry')}
                    </Button>
                </Group>
            </Stack>
        </Container>
    );
}