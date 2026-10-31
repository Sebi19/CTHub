import { useCallback, useEffect, useState } from 'react';
import { Alert, Center, Container, Loader, Stack, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { client } from '../../api.ts';
import type { AccountSecurityDto, SessionDto } from '../../api/generated.ts';
import { DeleteAccountCard, EmailCard } from './EmailCard.tsx';
import { PasskeysCard } from './PasskeysCard.tsx';
import { PasswordCard } from './PasswordCard.tsx';
import { SessionsCard } from './SessionsCard.tsx';
import { TotpCard } from './TotpCard.tsx';

export const ACCOUNT_PAGE_PATH = '/account';
export const CONFIRM_EMAIL_PATH = '/account/confirm-email';

export function AccountSecurityPage() {
    const { t } = useTranslation();
    useDocumentTitle(t('app.account.doc_title'));
    const [security, setSecurity] = useState<AccountSecurityDto | null>(null);
    const [sessions, setSessions] = useState<SessionDto[]>([]);
    const [failed, setFailed] = useState(false);

    const load = useCallback(async () => {
        try {
            const [sec, ses] = await Promise.all([client.api.getAccountSecurity(), client.api.listSessions()]);
            setSecurity(sec.data);
            setSessions(ses.data);
            setFailed(false);
        } catch {
            setFailed(true);
        }
    }, []);

    useEffect(() => {
        void load();
    }, [load]);

    const reload = () => void load();

    return (
        <Container size="sm" py="xl">
            <Stack gap="lg">
                <Title order={2}>{t('app.account.title')}</Title>
                {failed && <Alert color="red">{t('app.account.errors.generic')}</Alert>}
                {!security && !failed && <Center><Loader /></Center>}
                {security && (
                    <>
                        <PasskeysCard passkeys={security.passkeys} onChange={reload} />
                        <PasswordCard passwordSet={!!security.passwordSet} totpEnabled={!!security.totpEnabled}
                                      onChange={reload} />
                        <TotpCard enabled={!!security.totpEnabled} available={security.totpAvailable !== false}
                                  passwordSet={!!security.passwordSet} onChange={reload} />
                        <EmailCard />
                        <SessionsCard sessions={sessions} onChange={reload} />
                        <DeleteAccountCard />
                    </>
                )}
            </Stack>
        </Container>
    );
}
