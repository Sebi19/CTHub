import {Avatar, type AvatarProps, Tooltip} from "@mantine/core";
import {useTranslation} from "react-i18next";
import {IconUsers} from "@tabler/icons-react";

interface ProfileAvatarProps extends Omit<AvatarProps, 'children'> {
    avatarUrl?: string;
    hideNoImage?: boolean;
    profileAvailable?: boolean;
}

export const ProfileAvatar = ({ avatarUrl, size = "md", hideNoImage = false, profileAvailable = true, ...others }: ProfileAvatarProps) => {
    const { t } = useTranslation();

    if (!avatarUrl && hideNoImage) {
        return null;
    }

    const label = profileAvailable ? t("app.common.team.avatar.noImage") : t("app.common.team.avatar.noProfile");

    return (
        <Tooltip label={label} disabled={!!avatarUrl} zIndex={2000}>
            <Avatar src={avatarUrl} radius="50%" size={size} color="blue" variant="light"  {...others}>
                <IconUsers size="60%" />
            </Avatar>
        </Tooltip>
    );
}