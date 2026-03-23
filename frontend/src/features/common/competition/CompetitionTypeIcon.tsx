import {getCompetitionTypeColor, getCompetitionTypeIcon} from "../../../utils/competitionUtils.ts";
import {ThemeIcon, type ThemeIconProps} from "@mantine/core";
import type {CompetitionType} from "../../../api/generated.ts";

interface CompetitionTypeIconProps extends Omit<ThemeIconProps, 'children'> {
    type: CompetitionType;
}

export const CompetitionTypeIcon = ({ type, size = 40, ...others }: CompetitionTypeIconProps) => {
    const color = getCompetitionTypeColor(type);

    const IconComponent = getCompetitionTypeIcon(type);

    return (
        <ThemeIcon size={size} radius="xl" variant={"light"} color={color} {...others}>
            <IconComponent size="50%" />
        </ThemeIcon>
    )
}