import {Badge, type BadgeProps} from "@mantine/core";
import {getCountryFlagEmoji} from "../../utils/competitionUtils.ts";

interface CountryBadgeProps extends Omit<BadgeProps, 'children'> {
    country?: string;
}

export const CountryBadge = ({ country, ...others }: CountryBadgeProps) => {
    if (!country) {
        return null;
    }

    const countryFlag = getCountryFlagEmoji(country);
    const countryString = `${countryFlag} ${country}`;

    return (
        <Badge variant="outline" color="gray" {...others}>
            {countryString}
        </Badge>
    )
}