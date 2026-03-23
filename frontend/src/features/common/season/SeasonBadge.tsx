    import type {SeasonDto} from "../../../api/generated.ts";
    import {Badge, type BadgeProps} from "@mantine/core";
    import {useTranslation} from "react-i18next";

    interface SeasonBadgeProps extends Omit<BadgeProps, 'children'> {
        season: SeasonDto;
        hideIfActive?: boolean;
    }

    export const SeasonBadge = ({ season, hideIfActive = false, ...others }: SeasonBadgeProps) => {
        const {t} = useTranslation();

        if (hideIfActive && season.active) {
            return null;
        }

        return (
            <Badge color="blue" {...others}>
                {t('app.competition.detail.season', {
                    seasonName: season.name,
                    seasonId: season.id
                })}
            </Badge>
        );
    }