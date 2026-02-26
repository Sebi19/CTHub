package org.cthub.backend.mapper;

import org.cthub.backend.dto.common.LinkDto;
import org.cthub.backend.dto.team.SeasonTeamDto;
import org.cthub.backend.dto.team.TeamProfileDto;
import org.cthub.backend.model.Link;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.model.TeamProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeasonTeamMapper {
    @Mapping(target = "profile", source = "teamProfile")
    SeasonTeamDto toTeamDto(SeasonTeam seasonTeam);

    List<SeasonTeamDto> toTeamDtoList(List<SeasonTeam> seasonTeams);

    @Mapping(target = "profileName", source = "displayName")       // Assuming entity has 'private String name;'
    @Mapping(target = "profileUrl", source = "customUrl")     // Assuming entity has 'private String urlPart;'
    TeamProfileDto toProfileDto(TeamProfile profile);

    LinkDto toLinkDto(Link link);

    List<LinkDto> toLinkDtoList(List<Link> links);
}
