import {Avatar, type AvatarProps, Tooltip} from "@mantine/core";
import {IconUsers} from "@tabler/icons-react";
import {useTranslation} from "react-i18next";
import {ProfileAvatar} from "./ProfileAvatar.tsx";
import type {SeasonTeamSearchResultDto} from "../../../../api/generated.ts";

interface SeasonTeamAvatarProps extends Omit<AvatarProps, 'children'> {
    team: SeasonTeamSearchResultDto;
    hideNoProfile?: boolean;
    hideNoImage?: boolean;
    showBlue?: boolean;
}

export const SeasonTeamAvatar = ({
    team,
    size = "md",
    hideNoProfile = false,
    hideNoImage = false,
    showBlue = false,
    ...others
}: SeasonTeamAvatarProps) => {
    const { t } = useTranslation();

    const profile = team.seasonTeamProfile;

    if (!profile && !showBlue) {
        if (hideNoProfile || hideNoImage) return null;

        return (
            <Tooltip label={t("app.common.team.avatar.noProfile")} zIndex={2000}>
                <Avatar radius="50%" size={size} color="gray" variant="transparent" opacity={0.3} {...others}>
                    <IconUsers size="60%" />
                </Avatar>
            </Tooltip>
        );
    }

    return (
        <ProfileAvatar avatarUrl={profile?.seasonAvatarUrl} size={size} hideNoImage={hideNoImage} profileAvailable={!!profile} {...others} />
    );
}