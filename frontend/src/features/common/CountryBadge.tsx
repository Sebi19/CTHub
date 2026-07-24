import {Badge, type BadgeProps, Tooltip} from "@mantine/core";
import {getCountryFlagEmoji} from "../../utils/competitionUtils.ts";
import {useTranslation} from "react-i18next";

interface CountryBadgeProps extends Omit<BadgeProps, 'children'> {
    country?: string;
}

export const CountryBadge = ({ country, ...others }: CountryBadgeProps) => {
    if (!country) {
        return null;
    }

    const {t} = useTranslation();

    const countryFlag = getCountryFlagEmoji(country);
    const countryString = `${countryFlag} ${country}`;

    return (
        <Tooltip label={t("app.competition.teams.country", {context: country})}>
            <Badge variant="outline" color="gray" {...others}>
                {countryString}
            </Badge>
        </Tooltip>

    )
}