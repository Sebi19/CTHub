import { useEffect, useState } from 'react';
import { Center, Image, Loader, Modal, Stack, Text } from '@mantine/core';
import QRCode from 'qrcode';
import { useTranslation } from 'react-i18next';
import { phoneInstallUrl } from './pwaInstall.ts';

/** QR code that opens CT-Hub on a phone, with the install banner shown. */
export function PhoneQrCode({ size = 180 }: { size?: number }) {
    const { t } = useTranslation();
    const [src, setSrc] = useState<string | null>(null);

    useEffect(() => {
        let active = true;
        QRCode.toDataURL(phoneInstallUrl(), { margin: 1, width: size * 2 })
            .then((url) => {
                if (active) setSrc(url);
            })
            .catch(() => undefined);
        return () => {
            active = false;
        };
    }, [size]);

    return src
        ? <Image src={src} alt={t('app.pwa.qr.alt')} w={size} h={size} radius="sm" bg="white" p={4} />
        : <Center w={size} h={size}><Loader size="sm" /></Center>;
}

export function PhoneQrModal({ opened, onClose }: { opened: boolean; onClose: () => void }) {
    const { t } = useTranslation();
    return (
        <Modal opened={opened} onClose={onClose} title={t('app.pwa.qr.title')} centered>
            <Stack align="center" gap="md">
                <Text size="sm" c="dimmed" ta="center">{t('app.pwa.qr.description')}</Text>
                {opened && <PhoneQrCode size={220} />}
            </Stack>
        </Modal>
    );
}
