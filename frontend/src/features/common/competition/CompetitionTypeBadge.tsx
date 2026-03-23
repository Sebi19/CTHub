import {getCompetitionTypeColor} from "../../../utils/competitionUtils.ts";
import {Badge, type BadgeProps} from "@mantine/core";
import {useTranslation} from "react-i18next";
import type {CompetitionType} from "../../../api/generated.ts";

interface CompetitionTypeBadgeProps extends Omit<BadgeProps, 'children'> {
    type: CompetitionType;
}

export const CompetitionTypeBadge = ({ type, ...others }: CompetitionTypeBadgeProps) => {
    const {t} = useTranslation();

    return (
        <Badge color={getCompetitionTypeColor(type)} {...others}>
            {t(`app.competition.detail.type`, {context: type})}
        </Badge>
    )
}