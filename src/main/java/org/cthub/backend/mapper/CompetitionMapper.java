package org.cthub.backend.mapper;

import org.cthub.backend.dto.competition.CompetitionContactInfoDto;
import org.cthub.backend.dto.competition.CompetitionDetailDto;
import org.cthub.backend.dto.competition.CompetitionNominationDto;
import org.cthub.backend.dto.competition.CompetitionPlaceDto;
import org.cthub.backend.dto.competition.CompetitionRobotGameEntryDto;
import org.cthub.backend.dto.competition.CompetitionShortInfoDto;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Nomination;
import org.cthub.backend.model.Place;
import org.cthub.backend.model.RobotGameResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {SeasonTeamMapper.class, SeasonMapper.class})
public interface CompetitionMapper {

    @Mapping(target = "results", ignore = true)
    @Mapping(target = "registeredTeams", ignore = true)
    @Mapping(target = "contactInfo", source = "contact")
    @Mapping(target = "nextCompetition", ignore = true)
    @Mapping(target = "previousCompetitions", ignore = true)
    CompetitionDetailDto toBaseDetailDto(Competition competition);

    CompetitionShortInfoDto toShortInfoDto(Competition competition);

    @Mapping(target = "teamId", source = "seasonTeam.id")
    CompetitionPlaceDto toPlaceDto(Place place);

    @Mapping(target = "teamId", source = "seasonTeam.id")
    @Mapping(target = "isWinner", source = "awardWinner")
    CompetitionNominationDto toNominationDto(Nomination nomination);

    @Mapping(target = "teamId", source = "seasonTeam.id")
    CompetitionRobotGameEntryDto toRobotGameEntryDto(RobotGameResult result);

    CompetitionContactInfoDto toContactInfoDto(Competition.ContactInfo contactInfo);
}
