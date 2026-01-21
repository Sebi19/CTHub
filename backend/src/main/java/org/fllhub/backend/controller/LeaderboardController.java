package org.fllhub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.fllhub.backend.dto.OverallRobotGameEntryDto;
import org.fllhub.backend.service.LeaderboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/overall-robot-game")
    public List<OverallRobotGameEntryDto> getOverallRobotGameLeaderboard(
        @RequestParam Optional<String> seasonId) {
        if (seasonId.isPresent()) {
            return leaderboardService.getGlobalLeaderboard(seasonId.get());
        } else {
            return leaderboardService.getGlobalLeaderboardActiveSeason();
        }
    }
}