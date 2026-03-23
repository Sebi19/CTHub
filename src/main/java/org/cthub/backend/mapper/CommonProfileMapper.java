package org.cthub.backend.mapper;

import org.cthub.backend.dto.team.SeasonTeamProfileDto;
import org.cthub.backend.dto.team.TeamProfileDto;
import org.cthub.backend.model.SeasonTeamProfile;
import org.cthub.backend.model.TeamProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CommonProfileMapper {
    @Mapping(target = "seasonAvatarUrl", source = "seasonTeamProfile", qualifiedByName = "seasonTeamProfileToAvatarUrl")
    @Mapping(target = "profile", source = "teamProfile")
    SeasonTeamProfileDto toSeasonTeamProfileDto(SeasonTeamProfile seasonTeamProfile);

    @Mapping(target = "profileName", source = "displayName")
    @Mapping(target = "profileUrl", source = "customUrl")
    @Mapping(target = "avatarUrl", source = "profileImageUuid", qualifiedByName = "uuidToAvatarUrl")
    TeamProfileDto toProfileDto(TeamProfile profile);

    @Named("uuidToAvatarUrl")
    default String mapUuidToAvatarUrl(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return "/api/images/" + uuid;
    }

    @Named("seasonTeamProfileToAvatarUrl")
    default String mapSeasonTeamProfileToAvatarUrl(SeasonTeamProfile seasonTeamProfile) {
        if (seasonTeamProfile == null) {
            return null;
        }
        return switch (seasonTeamProfile.getAvatarMode()) {
            case CUSTOM -> mapUuidToAvatarUrl(seasonTeamProfile.getCustomAvatarId());
            case INHERIT -> mapUuidToAvatarUrl(seasonTeamProfile.getTeamProfile().getProfileImageUuid());
            default -> null;
        };
    }
}
