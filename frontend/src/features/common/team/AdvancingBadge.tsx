import {Badge, type BadgeProps, Tooltip} from "@mantine/core";
import {Link} from "react-router-dom";
import {getCompetitionLink} from "../../../utils/routingUtils.ts";
import {IconArrowUpRight} from "@tabler/icons-react";
import type {CompetitionShortInfoDto} from "../../../api/generated.ts";
import {useTranslation} from "react-i18next";
import React from "react";

interface AdvancingBadgeProps extends Omit<BadgeProps, 'children'> {
    competitionType: string;
    advancing?: boolean;
    nextCompetition?: CompetitionShortInfoDto;
}

export const AdvancingBadge = ({ competitionType, advancing, size = 'md', nextCompetition, ...others }: AdvancingBadgeProps) => {
    const {t} = useTranslation();

    if (!advancing || competitionType === 'FINAL') return null;

    const commonBadgeProps = {
        color: "green",
        variant: "light",
        size,
        rightSection: <IconArrowUpRight size={14} />,
        onClick: (e: React.MouseEvent) => e.stopPropagation(), // Inline is perfectly fine here
        ...others,
    };

    const label = t("app.competition.awards.qualified");

    const badgeElement = nextCompetition ? (
        <Badge
            component={Link}
            to={getCompetitionLink(nextCompetition)}
            style={{ cursor: 'pointer' }}
            {...commonBadgeProps}
        >
            {label}
        </Badge>
    ) : (
        <Badge {...commonBadgeProps}>
            {label}
        </Badge>
    );

    return (
        <Tooltip
            label={nextCompetition?.name ?? ''}
            disabled={!nextCompetition}
        >
            {badgeElement}
        </Tooltip>
    );
};