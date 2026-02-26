package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.robotgame.OverallRobotGameEntryDto;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.repository.RobotGameResultRepository;
import org.cthub.backend.repository.SeasonRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final RobotGameResultRepository robotGameResultRepository;

    private final SeasonRepository seasonRepository;

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
                return convertToDto(result, qualified);
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

    private OverallRobotGameEntryDto convertToDto(RobotGameResult result, boolean qualified) {
        // Collect all non-null scores to calculate stats (Avg/Median)
        // Note: We include finals in the stats? Typically FLL stats (Median/Avg)
        // rely ONLY on preliminary rounds to keep it fair for everyone.
        // Let's stick to Prelims for Avg/Median, but use ALL for Best/Worst.

        List<Integer> prelims = Stream.of(result.getPr1(), result.getPr2(), result.getPr3())
            .filter(s -> s > 0) // filter out non-positive scores
            .collect(Collectors.toList());

        List<Integer> allScores = new ArrayList<>(prelims);
        if (result.getR16() != null) allScores.add(result.getR16());
        if (result.getQf() != null) allScores.add(result.getQf());
        if (result.getSf() != null) allScores.add(result.getSf());
        if (result.getF1() != null) allScores.add(result.getF1());
        if (result.getF2() != null) allScores.add(result.getF2());

        // --- Math Time 🧮 ---
        int best = allScores.isEmpty() ? 0 : Collections.max(allScores);
        int worst = allScores.isEmpty() ? 0 : Collections.min(allScores);

        // Median/Avg usually calculated on Prelims only for consistency
        double avg = calculateAverage(allScores);
        double median = calculateMedian(allScores);

        return OverallRobotGameEntryDto.builder()
            .rank(0) // Set in the main method
            .teamName(result.getSeasonTeam().getName())
            .teamId(result.getSeasonTeam().getFllId()) // Ensure this exists on SeasonTeam
            .competition(result.getCompetition().getName())
            .competitionUrlPart(result.getCompetition().getUrlPart()) // e.g. "ortenau"
            .country(result.getSeasonTeam().getCountry())
            .qualified(qualified)

            // Calculated Stats
            .bestScore(best)
            .worstScore(worst)
            .averageScore(Math.round(avg * 10.0) / 10.0) // 1 decimal place
            .medianScore(median)

            // Raw Data
            .preliminaryRound1(result.getPr1())
            .preliminaryRound2(result.getPr2())
            .preliminaryRound3(result.getPr3())
            .bestPreliminaryScore(result.getBestPr()) // Using your persisted field!

            .roundOf16(result.getR16())
            .quarterFinal(result.getQf())
            .semiFinal(result.getSf())
            .final1(result.getF1())
            .final2(result.getF2())
            .build();
    }

    private double calculateMedian(List<Integer> scores) {
        if (scores.isEmpty()) return 0.0;
        List<Integer> sorted = new ArrayList<>(scores);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private double calculateAverage(List<Integer> scores) {
        if (scores.isEmpty()) return 0.0;
        double sum = 0.0;
        for (int score : scores) {
            sum += score;
        }
        return sum / scores.size();
    }
}