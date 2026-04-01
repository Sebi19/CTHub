    import type {SeasonDto} from "../../../api/generated.ts";
    import {Badge, type BadgeProps} from "@mantine/core";
    import {useTranslation} from "react-i18next";

    interface SeasonBadgeProps extends Omit<BadgeProps, 'children'> {
        season: SeasonDto;
        hideIfActive?: boolean;
        short?: boolean;
    }

    export const SeasonBadge = ({ season, hideIfActive = false, short = false, ...others }: SeasonBadgeProps) => {
        const {t} = useTranslation();

        if (hideIfActive && season.active) {
            return null;
        }

        const translation = short ? 'app.competition.detail.season_short' : 'app.competition.detail.season';

        return (
            <Badge color="blue" {...others}>
                {t(translation, {
                    seasonName: season.name,
                    seasonId: season.id
                })}
            </Badge>
        );
    }