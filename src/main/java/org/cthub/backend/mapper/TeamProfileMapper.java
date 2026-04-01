package org.cthub.backend.mapper;

import org.cthub.backend.dto.search.TeamProfileSearchResultDto;
import org.cthub.backend.dto.team.SeasonTeamDetailsDto;
import org.cthub.backend.dto.team.TeamProfileDetailsDto;
import org.cthub.backend.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CommonProfileMapper.class})
public interface TeamProfileMapper {
    @Mapping(target = "avatarUrl", source = "profile.profileImageUuid", qualifiedByName = "uuidToAvatarUrl")
    @Mapping(target = "seasons", source = "seasons")
    @Mapping(target = "profileUrl", source = "profile.customUrl")
    @Mapping(target = "profileName", source = "profile.displayName")
    TeamProfileDetailsDto toDto(TeamProfile profile, List<SeasonTeamDetailsDto> seasons);

    @Mapping(target = "avatarUrl", source = "profile.profileImageUuid", qualifiedByName = "uuidToAvatarUrl")
    @Mapping(target = "profileUrl", source = "profile.customUrl")
    @Mapping(target = "profileName", source = "profile.displayName")
    TeamProfileSearchResultDto toSearchResultDto(TeamProfile profile);
}