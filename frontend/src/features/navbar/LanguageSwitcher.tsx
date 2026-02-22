import {Button, Tooltip} from '@mantine/core';
import { useTranslation } from 'react-i18next';
import {IconLanguage} from "@tabler/icons-react";
import {useMemo} from "react";

type LanguageSwitcherProps = {
    isMobile: boolean;
};

export function LanguageSwitcher({isMobile = false}: LanguageSwitcherProps) {
    const { i18n, t } = useTranslation();

    const isEnglish = i18n.language === 'en';
    const nextLang = isEnglish ? 'de' : 'en';

    const text = useMemo(() => {
        if (isMobile) {
            return t("app.sidebar.currentLanguage", {lng: nextLang});
        } else {
            return t("app.header.currentLanguage", {lng: nextLang})
        }
    }, [isMobile, t])

    return (
        <Tooltip label={t("app.header.toggleLanguage", {lng: nextLang})}>
            <Button
                variant={"default"}
                onClick={() => {
                    i18n.changeLanguage(nextLang);
                }}
                justify={"flex-start"}
                leftSection={<IconLanguage/>}
            >{text}
            </Button>
        </Tooltip>
    );
}