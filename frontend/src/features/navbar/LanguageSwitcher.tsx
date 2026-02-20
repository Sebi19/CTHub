import {Button, Tooltip} from '@mantine/core';
import { useTranslation } from 'react-i18next';

export function LanguageSwitcher() {
    const { i18n, t } = useTranslation();

    const isEnglish = i18n.language === 'en';
    const nextLang = isEnglish ? 'de' : 'en';

    return (
        <Tooltip label={t("app.header.toggleLanguage")}>
            <Button
                variant={"default"}
                onClick={() => {
                    i18n.changeLanguage(nextLang);
                }}>{nextLang.toUpperCase()}
            </Button>
        </Tooltip>
    );
}