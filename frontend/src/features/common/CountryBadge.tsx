import {Badge, type BadgeProps, Tooltip} from "@mantine/core";
import {getCountryFlagUrl} from "../../utils/competitionUtils.ts";
import {useTranslation} from "react-i18next";
import {CountryFlag} from "./CountryFlag.tsx";

interface CountryBadgeProps extends Omit<BadgeProps, 'children'> {
    country?: string;
}

export const CountryBadge = ({ country, ...others }: CountryBadgeProps) => {
    const {t} = useTranslation();

    if (!country) {
        return null;
    }

    return (
        <Tooltip label={t("app.competition.teams.country", {context: country})}>
            <Badge
                variant="outline"
                color="gray"
                leftSection={getCountryFlagUrl(country) && <CountryFlag country={country} />}
                {...others}
            >
                {country}
            </Badge>
        </Tooltip>

    )
}