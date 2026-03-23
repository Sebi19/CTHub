import {Avatar, type AvatarProps, Tooltip} from "@mantine/core";
import {useTranslation} from "react-i18next";
import {IconUsers} from "@tabler/icons-react";

interface ProfileAvatarProps extends Omit<AvatarProps, 'children'> {
    avatarUrl?: string;
    hideNoImage?: boolean;
}

export const ProfileAvatar = ({ avatarUrl, size = "md", hideNoImage = false, ...others }: ProfileAvatarProps) => {
    const { t } = useTranslation();

    if (!avatarUrl && hideNoImage) {
        return null;
    }

    return (
        <Tooltip label={t("app.common.team.avatar.noImage")} disabled={!!avatarUrl}>
            <Avatar src={avatarUrl} radius="50%" size={size} color="blue" variant="light"  {...others}>
                <IconUsers size="60%" />
            </Avatar>
        </Tooltip>
    );
}