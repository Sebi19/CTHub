package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.team.SeasonTeamDetailsDto;
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
public class SeasonTeamService {

    private final SeasonTeamRepository seasonTeamRepository;
    private final PlaceRepository placeRepository;
    private final NominationRepository nominationRepository;
    private final RobotGameResultRepository robotGameResultRepository;
    private final CompetitionRepository competitionRepository;
    private final SeasonTeamMapper teamMapper;

    @Transactional(readOnly = true)
    public SeasonTeamDetailsDto getSeasonTeamDetails(String seasonId, String fllId) {
        SeasonTeam seasonTeam = seasonTeamRepository.findBySeasonIdAndFllIdWithDetails(seasonId, fllId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SeasonTeam not found for seasonId: " + seasonId + " and fllId: " + fllId));

        Long teamId = seasonTeam.getId();

        List<Place> places = placeRepository.findBySeasonTeamId(teamId);
        List<Nomination> nominations = nominationRepository.findBySeasonTeamIdWithCompetition(teamId);
        List<RobotGameResult> robotGameResults = robotGameResultRepository.findBySeasonTeamIdWithCompetition(teamId);
        List<Competition> nextCompetitions = competitionRepository.findAllBySeasonIdAndUrlPart(seasonId, seasonTeam.getRegisteredCompetitions().stream().map(Competition::getQualificationUrlPart).toList());

        return teamMapper.toTeamDetailsDto(seasonTeam, places, nominations, robotGameResults, nextCompetitions);
    }
}
