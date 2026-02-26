package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.competition.CompetitionDetailDto;
import org.cthub.backend.dto.competition.CompetitionResultsDto;
import org.cthub.backend.mapper.CompetitionMapper;
import org.cthub.backend.mapper.SeasonTeamMapper;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Nomination;
import org.cthub.backend.model.Place;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.NominationRepository;
import org.cthub.backend.repository.PlaceRepository;
import org.cthub.backend.repository.RobotGameResultRepository;
import org.cthub.backend.repository.SeasonTeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final PlaceRepository placeRepository;
    private final NominationRepository nominationRepository;
    private final RobotGameResultRepository robotGameResultRepository;
    private final CompetitionMapper competitionMapper;
    private final SeasonTeamMapper teamMapper;


    @Transactional(readOnly = true)
    public CompetitionDetailDto getCompetitionDetails(String seasonId, String urlPart) {

        // 1. Get the base competition using BOTH identifiers
        Competition competition = competitionRepository.findByUrlPartAndSeasonId(urlPart, seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));

        Long compId = competition.getId();
        CompetitionDetailDto detailDto = competitionMapper.toBaseDetailDto(competition);

        // 2. Fetch SeasonTeams and map them to TeamDtos
        List<SeasonTeam> seasonTeams = seasonTeamRepository.findRegisteredTeamsByCompetitionId(compId);
        detailDto.setRegisteredTeams(teamMapper.toTeamDtoList(seasonTeams));

        if (competition.isResultsAvailable()) {
            List<Place> places = placeRepository.findAllByCompetitionId(compId);
            List<Nomination> nominations = nominationRepository.findAllByCompetitionId(compId);
            List<RobotGameResult> robotGames = robotGameResultRepository.findAllByCompetitionId(compId);

            CompetitionResultsDto resultsDto = CompetitionResultsDto.builder()
                .places(places.stream().map(competitionMapper::toPlaceDto).toList())
                .nominations(nominations.stream().map(competitionMapper::toNominationDto).toList())
                .robotGameEntries(robotGames.stream().map(competitionMapper::toRobotGameEntryDto).toList())
                .build();

            detailDto.setResults(resultsDto);
        }

        return detailDto;
    }
}
