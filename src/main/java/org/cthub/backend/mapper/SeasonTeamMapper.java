package org.cthub.backend.mapper;

import org.cthub.backend.dto.common.LinkDto;
import org.cthub.backend.dto.competition.CompetitionNominationDto;
import org.cthub.backend.dto.competition.CompetitionPlaceDto;
import org.cthub.backend.dto.competition.CompetitionRobotGameEntryDto;
import org.cthub.backend.dto.competition.CompetitionShortInfoDto;
import org.cthub.backend.dto.search.SeasonTeamSearchResultDto;
import org.cthub.backend.dto.team.SeasonTeamDetailsDto;
import org.cthub.backend.dto.team.SeasonTeamDto;
import org.cthub.backend.dto.team.SeasonTeamSummaryDto;
import org.cthub.backend.dto.team.TeamCompetitionRecordDto;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Link;
import org.cthub.backend.model.Nomination;
import org.cthub.backend.model.Place;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.model.SeasonTeam;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {CommonProfileMapper.class, SeasonMapper.class})
public abstract class SeasonTeamMapper {

    @Lazy
    @Autowired
    protected CompetitionMapper competitionMapper;

    @Mapping(target = "seasonTeamProfile", source = "seasonTeamProfile")
    public abstract SeasonTeamDto toTeamDto(SeasonTeam seasonTeam);

    @Mapping(target = "seasonTeamProfile", source = "seasonTeamProfile")
    public abstract SeasonTeamSummaryDto toTeamSummaryDto(SeasonTeam seasonTeam);

    @Mapping(target = "seasonTeamProfile", source = "seasonTeamProfile")
    @Mapping(target = "competitionRecords", source = "seasonTeam")
    public abstract SeasonTeamDetailsDto toTeamDetailsDto(
        SeasonTeam seasonTeam,
        @Context List<Place> places,
        @Context List<Nomination> nominations,
        @Context List<RobotGameResult> results,
        @Context List<Competition> nextCompetitions
    );

    public abstract List<SeasonTeamDto> toTeamDtoList(List<SeasonTeam> seasonTeams);

    @Mapping(target = "seasonTeamProfile", source = "seasonTeamProfile")
    public abstract SeasonTeamSearchResultDto toSearchResultDto(SeasonTeam seasonTeam);

    protected abstract LinkDto toLinkDto(Link link);

    protected abstract List<LinkDto> toLinkDtoList(List<Link> links);

    protected List<TeamCompetitionRecordDto> mapCompetitionRecords(
        SeasonTeam team,
        @Context List<Place> places,
        @Context List<Nomination> nominations,
        @Context List<RobotGameResult> rgResults,
        @Context List<Competition> nextCompetitions) {

        return team.getRegisteredCompetitions().stream()
            .map(competition -> {
                CompetitionShortInfoDto shortInfo = competitionMapper.toShortInfoDto(competition);

                CompetitionShortInfoDto nextCompShortInfo = nextCompetitions.stream()
                    .filter(nc -> nc.getUrlPart() != null && nc.getSeason().getId().equals(competition.getSeason().getId()) && nc.getUrlPart().equals(competition.getQualificationUrlPart()))
                    .findFirst()
                    .map(competitionMapper::toShortInfoDto)
                    .orElse(null);

                CompetitionPlaceDto placeDto = places.stream()
                    .filter(p -> p.getCompetition().getId().equals(competition.getId()))
                    .findFirst()
                    .map(competitionMapper::toPlaceDto)
                    .orElse(null);

                List<CompetitionNominationDto> nominationDtos = nominations.stream()
                    .filter(n -> n.getCompetition().getId().equals(competition.getId()))
                    .map(competitionMapper::toNominationDto)
                    .collect(Collectors.toList());

                CompetitionRobotGameEntryDto rgDto = rgResults.stream()
                    .filter(rg -> rg.getCompetition().getId().equals(competition.getId()))
                    .findFirst()
                    .map(competitionMapper::toRobotGameEntryDto)
                    .orElse(null);

                return TeamCompetitionRecordDto.builder()
                    .competition(shortInfo)
                    .place(placeDto)
                    .nominations(nominationDtos)
                    .robotGame(rgDto)
                    .nextCompetition(nextCompShortInfo)
                    .build();
            })
            .collect(Collectors.toList());
    }
}
