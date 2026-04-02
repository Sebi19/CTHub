package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.robotgame.OverallRobotGameEntryDto;
import org.cthub.backend.mapper.RobotGameEntryMapper;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.repository.RobotGameResultRepository;
import org.cthub.backend.repository.SeasonRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final RobotGameResultRepository robotGameResultRepository;
    private final SeasonRepository seasonRepository;
    private final RobotGameEntryMapper robotGameEntryMapper;

    @Cacheable("globalLeaderboardActiveSeason")
    public List<OverallRobotGameEntryDto> getGlobalLeaderboardActiveSeason() {
        String currentSeasonId = seasonRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalArgumentException("No active season found"))
                .getId();
        return getGlobalLeaderboard(currentSeasonId);
    }

    public List<OverallRobotGameEntryDto> getGlobalLeaderboard(String seasonId) {
        // 1. Fetch data (Optimized query later, findAll for now)
        List<Object[]> rawResults = robotGameResultRepository.findAllBySeasonIdWithQualification(seasonId);

        List<OverallRobotGameEntryDto> leaderboard = rawResults.stream()
            .map(row -> {
                RobotGameResult result = (RobotGameResult) row[0];
                boolean qualified = (Boolean) row[1];
                return robotGameEntryMapper.toOverallRobotGameEntryDto(result, qualified);
            })
            // Sort by absolute best score descending
            .sorted(Comparator.comparingInt(OverallRobotGameEntryDto::getBestScore).reversed())
            .collect(Collectors.toList());

        // 2. Assign Ranks (1, 2, 3...) based on sorted order
        for (int i = 0; i < leaderboard.size(); i++) {
            if (i > 0 && leaderboard.get(i).getBestScore() == leaderboard.get(i - 1).getBestScore()) {
                // Tie: same rank as previous
                leaderboard.get(i).setRank(leaderboard.get(i - 1).getRank());
            } else {
                leaderboard.get(i).setRank(i + 1);
            }
        }

        return leaderboard;
    }
}