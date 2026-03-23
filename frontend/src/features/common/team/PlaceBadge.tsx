import {Badge, Tooltip, Text, type BadgeProps, type MantineSize} from '@mantine/core';
import {IconTrophy, IconMedal, IconHash, type TablerIcon} from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import type {CompetitionPlaceDto} from "../../../api/generated.ts";

interface PlaceBadgeProps extends Omit<BadgeProps, 'children' | 'size'> {
    placeObj?: CompetitionPlaceDto;
    displayStyle?: 'full' | 'compact';
    size?: MantineSize;
}

const iconSizeMap: Record<string, number> = {
    xs: 10,
    sm: 12,
    md: 14,
    lg: 16,
    xl: 20,
};

export const PlaceBadge = ({ placeObj, displayStyle = 'compact', size = "lg", ...others }: PlaceBadgeProps) => {
    const { t } = useTranslation();

    if (!placeObj) return <Text c="dimmed">-</Text>;

    const { place } = placeObj;

    const iconSize = iconSizeMap[size] || 16;

    const color = place === 1 ? 'red' : place === 2 ? 'gray' : place === 3 ? 'orange' : 'gray';
    const variant = place === 1 ? "outline" : "light";

    const PlaceIcon: TablerIcon = place === 1
        ? IconTrophy
        : place === 2 || place === 3
            ? IconMedal
            : IconHash;

    const placeLabel = t("app.competition.awards.place", { count: place, ordinal: true })

    return (
        <Tooltip
            label={placeLabel}
            disabled={displayStyle !== 'compact'}
        >
            <Badge
                size={size}
                leftSection={<PlaceIcon size={iconSize}/>}
                color={color}
                variant={variant}
                tt="none"
                {...others}
            >
                {displayStyle === 'full' ? placeLabel : place}
            </Badge>
        </Tooltip>
    );
};