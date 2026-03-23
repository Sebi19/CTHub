package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.team.SeasonTeamDetailsDto;
import org.cthub.backend.dto.team.TeamProfileDetailsDto;
import org.cthub.backend.mapper.SeasonTeamMapper;
import org.cthub.backend.mapper.TeamProfileMapper;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Nomination;
import org.cthub.backend.model.Place;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.model.TeamProfile;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.NominationRepository;
import org.cthub.backend.repository.PlaceRepository;
import org.cthub.backend.repository.RobotGameResultRepository;
import org.cthub.backend.repository.SeasonTeamRepository;
import org.cthub.backend.repository.TeamProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamProfileService {
    private final TeamProfileRepository profileRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final CompetitionRepository competitionRepository;
    private final PlaceRepository placeRepository;
    private final NominationRepository nominationRepository;
    private final RobotGameResultRepository robotGameResultRepository;
    private final TeamProfileMapper profileMapper;
    private final SeasonTeamMapper seasonTeamMapper;

    @Transactional(readOnly = true)
    public TeamProfileDetailsDto getTeamProfileDetails(String profileUrl) {

        // 1. Fetch the Core Profile
        TeamProfile profile = profileRepository.findByCustomUrl(profileUrl)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TeamProfile not found"));

        // 2. Fetch all SeasonTeams linked to this profile
        List<SeasonTeam> seasonTeams = seasonTeamRepository.findByTeamProfileId(profile.getId());

        if (seasonTeams.isEmpty()) {
            // If they have no seasons yet, just return the empty profile!
            return profileMapper.toDto(profile, List.of());
        }

        // 3. Extract the IDs for our Batch Queries
        List<Long> teamIds = seasonTeams.stream().map(SeasonTeam::getId).toList();

        List<String> nextCompIdentifiers = seasonTeams.stream()
            .flatMap(team -> team.getRegisteredCompetitions().stream()
                .filter(comp -> comp.getQualificationUrlPart() != null) // Avoid nulls!
                .map(comp -> team.getSeason().getId() + "/" + comp.getQualificationUrlPart())
            )
            .distinct() // Remove duplicates if multiple teams went to the same next comp
            .toList();

        // 4. BATCH FETCH: Get EVERYTHING across all seasons in 3 queries
        List<Place> allPlaces = placeRepository.findBySeasonTeamIdIn(teamIds);
        List<Nomination> allNominations = nominationRepository.findBySeasonTeamIdInWithCompetition(teamIds);
        List<RobotGameResult> allResults = robotGameResultRepository.findBySeasonTeamIdInWithCompetition(teamIds);
        List<Competition> allNextCompetitions = competitionRepository.findAllByPairs(nextCompIdentifiers);

        // 5. Group the bulk data by SeasonTeam ID so we can map them accurately
        Map<Long, List<Place>> placesByTeam = allPlaces.stream()
            .collect(Collectors.groupingBy(p -> p.getSeasonTeam().getId()));

        Map<Long, List<Nomination>> nominationsByTeam = allNominations.stream()
            .collect(Collectors.groupingBy(n -> n.getSeasonTeam().getId()));

        Map<Long, List<RobotGameResult>> resultsByTeam = allResults.stream()
            .collect(Collectors.groupingBy(r -> r.getSeasonTeam().getId()));


        // 6. Map each SeasonTeam into a TeamProfileSeasonDto
        List<SeasonTeamDetailsDto> mappedSeasons = seasonTeams.stream().map(seasonTeam -> {
            Long id = seasonTeam.getId();
            // We reuse a method similar to your existing mapper, but passing the grouped data!
            return seasonTeamMapper.toTeamDetailsDto(
                seasonTeam,
                placesByTeam.getOrDefault(id, List.of()),
                nominationsByTeam.getOrDefault(id, List.of()),
                resultsByTeam.getOrDefault(id, List.of()),
                allNextCompetitions
            );
        }).toList();

        // 7. Assemble the final Profile DTO
        return profileMapper.toDto(profile, mappedSeasons);
    }
}
