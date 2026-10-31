import { Button, SimpleGrid, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';

interface Props {
    options: number[];
    device?: string;
    requestedAt?: string;
    busy?: boolean;
    onPick: (number: number) => void;
}

/** The three numbers; exactly one matches what the requesting browser shows. */
export function ChallengeNumberPicker({ options, device, requestedAt, busy, onPick }: Props) {
    const { t, i18n } = useTranslation();
    const when = requestedAt
        ? new Date(requestedAt).toLocaleTimeString(i18n.resolvedLanguage, { hour: '2-digit', minute: '2-digit' })
        : null;

    return (
        <Stack gap="sm">
            {(device || when) && (
                <Text size="sm" c="dimmed">
                    {t('app.challenge.request_context', {
                        device: device ?? t('app.challenge.unknown_device'),
                        time: when ?? '',
                    })}
                </Text>
            )}
            <Text size="sm" fw={500}>{t('app.challenge.pick_number')}</Text>
            <SimpleGrid cols={3} spacing="sm">
                {options.map((n) => (
                    <Button key={n} size="xl" variant="default" disabled={busy} onClick={() => onPick(n)}
                            style={{ fontSize: 28, fontVariantNumeric: 'tabular-nums' }}>
                        {String(n).padStart(2, '0')}
                    </Button>
                ))}
            </SimpleGrid>
        </Stack>
    );
}
