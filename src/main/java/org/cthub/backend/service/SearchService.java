package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.search.CompetitionSearchResultDto;
import org.cthub.backend.dto.search.SearchResultItemDto;
import org.cthub.backend.dto.search.SearchResultTypeDto;
import org.cthub.backend.dto.search.SeasonTeamSearchResultDto;
import org.cthub.backend.dto.search.TeamProfileSearchResultDto;
import org.cthub.backend.mapper.CompetitionMapper;
import org.cthub.backend.mapper.SeasonTeamMapper;
import org.cthub.backend.mapper.TeamProfileMapper;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.model.TeamProfile;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.SeasonTeamRepository;
import org.cthub.backend.repository.TeamProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final CompetitionRepository competitionRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final TeamProfileRepository teamProfileRepository;
    private final CompetitionMapper competitionMapper;
    private final SeasonTeamMapper seasonTeamMapper;
    private final TeamProfileMapper teamProfileMapper;

    public List<SearchResultItemDto> performGlobalSearch(String query, String seasonId) {
        double threshold = 0.15; // Set your fuzzy strictness here

        // 1. Fetch the raw Object[] arrays from both repositories
        List<Object[]> rawCompetitions = competitionRepository.searchFuzzyCompetitions(query, seasonId, threshold);
        List<Object[]> rawTeams = seasonTeamRepository.searchFuzzySeasonTeams(query, seasonId, threshold);
        List<Object[]> rawTeamProfiles = teamProfileRepository.searchFuzzyTeamProfiles(query, seasonId, threshold);

        // 2. Map both sets of results into Streams of our Envelope DTO
        Stream<SearchResultItemDto> competitionStream = rawCompetitions.stream()
            .map(this::mapToCompetitionResult);

        Stream<SearchResultItemDto> teamStream = rawTeams.stream()
            .map(this::mapToSeasonTeamResult);

        Stream<SearchResultItemDto> teamProfileStream = rawTeamProfiles.stream()
            .map(this::mapToTeamProfileResult);


        // 3. Combine, Sort, and Limit
        return Stream.of(competitionStream, teamStream, teamProfileStream)
            .flatMap(Function.identity()) // Flattens all 3 streams into 1 seamless stream
            .sorted(searchResultComparator())
            .limit(10)
            .collect(Collectors.toList());
    }

    private SearchResultItemDto mapToCompetitionResult(Object[] row) {
        Competition competition = (Competition) row[0];
        Double score = ((Number) row[1]).doubleValue();

        CompetitionSearchResultDto compDto = competitionMapper.toSearchResultDto(competition);

        return SearchResultItemDto.builder()
            .type(SearchResultTypeDto.COMPETITION)
            .score(score)
            .competition(compDto)
            .build();
    }

    private SearchResultItemDto mapToSeasonTeamResult(Object[] row) {
        // 1. Clean extraction
        SeasonTeam team = (SeasonTeam) row[0];
        Double score = ((Number) row[1]).doubleValue();

        // 2. MapStruct handles the DTO conversion (and has access to the eagerly fetched profiles!)
        SeasonTeamSearchResultDto seasonTeamDto = seasonTeamMapper.toSearchResultDto(team);

        // 3. Wrap it in the Envelope
        return SearchResultItemDto.builder()
            .type(SearchResultTypeDto.SEASON_TEAM)
            .score(score)
            .seasonTeam(seasonTeamDto)
            .build();
    }

    private SearchResultItemDto mapToTeamProfileResult(Object[] row) {
        // 1. Clean extraction
        TeamProfile teamProfile = (org.cthub.backend.model.TeamProfile) row[0];
        Double score = ((Number) row[1]).doubleValue();

        // 2. MapStruct handles the DTO conversion
        TeamProfileSearchResultDto teamProfileDto = teamProfileMapper.toSearchResultDto(teamProfile);

        // 3. Wrap it in the Envelope
        return SearchResultItemDto.builder()
            .type(SearchResultTypeDto.TEAM_PROFILE)
            .score(score)
            .teamProfile(teamProfileDto)
            .build();
    }

    private Comparator<SearchResultItemDto> searchResultComparator() {
        return (a, b) -> {
            // 1. PRIMARY TIEBREAKER: Match Score (Descending)
            int scoreCompare = Double.compare(Math.round(b.getScore() * 1000), Math.round(a.getScore() * 1000));
            if (scoreCompare != 0) return scoreCompare;

            // 2. SECONDARY TIEBREAKER: Result Type Rank
            // Competition (1) > Profile (2) > Season Team (3)
            int typeCompare = Integer.compare(getTypeRank(a.getType()), getTypeRank(b.getType()));
            if (typeCompare != 0) return typeCompare;

            // --- At this point, we know both items have the exact same TYPE and SCORE ---

            // 3. TERTIARY TIEBREAKER: Season Start Year (Descending)
            // (This only applies to Competitions and Teams; Profiles return 0 and skip this)
            int yearA = extractSeasonYear(a);
            int yearB = extractSeasonYear(b);
            int yearCompare = Integer.compare(yearB, yearA); // Descending!
            if (yearCompare != 0) return yearCompare;

            // 4. QUATERNARY TIEBREAKER: Entity-specific sorting
            if (a.getType() == SearchResultTypeDto.COMPETITION) {
                // Competitions: Sort by Date (Ascending / Older first)
                return compareSafely(
                    a.getCompetition().getDate(),
                    b.getCompetition().getDate()
                );
            }
            else if (a.getType() == SearchResultTypeDto.SEASON_TEAM) {
                // Season Teams: Sort by FLL ID (Ascending)
                // Note: If IDs are purely numeric but stored as Strings, "1000" will sort before "200".
                // If that's an issue, you can wrap this in Integer.parseInt() or pad them!
                return compareSafely(
                    a.getSeasonTeam().getFllId(),
                    b.getSeasonTeam().getFllId()
                );
            }
            else if (a.getType() == SearchResultTypeDto.TEAM_PROFILE) {
                // Team Profiles: Sort Alphabetically (Ascending)
                return compareSafely(
                    a.getTeamProfile().getProfileName(),
                    b.getTeamProfile().getProfileName()
                );
            }

            // Complete tie (Should practically never happen)
            return 0;
        };
    }

// --- Helper Methods to keep the Comparator clean ---

    private int getTypeRank(SearchResultTypeDto type) {
        if (type == SearchResultTypeDto.COMPETITION) return 1;
        if (type == SearchResultTypeDto.TEAM_PROFILE) return 2;
        if (type == SearchResultTypeDto.SEASON_TEAM) return 3;
        return 99; // Fallback
    }

    private int extractSeasonYear(SearchResultItemDto item) {
        if (item.getType() == SearchResultTypeDto.COMPETITION && item.getCompetition() != null) {
            return item.getCompetition().getSeason().getStartYear();
        }
        if (item.getType() == SearchResultTypeDto.SEASON_TEAM && item.getSeasonTeam() != null) {
            return item.getSeasonTeam().getSeason().getStartYear();
        }
        return 0; // Profiles don't have seasons attached directly to the search envelope
    }

    /**
     * Null-safe generic comparator for Dates, Strings, etc.
     * Puts null values at the very bottom of the list.
     */
    private <T extends Comparable<T>> int compareSafely(T valA, T valB) {
        if (valA == null && valB == null) return 0;
        if (valA == null) return 1;
        if (valB == null) return -1;
        return valA.compareTo(valB);
    }
}
