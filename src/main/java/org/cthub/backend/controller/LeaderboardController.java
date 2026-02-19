package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.OverallRobotGameEntryDto;
import org.cthub.backend.service.LeaderboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/overall-robot-game")
    @PreAuthorize("permitAll()")
    public List<OverallRobotGameEntryDto> getOverallRobotGameLeaderboard(
        @RequestParam Optional<String> seasonId) {
        if (seasonId.isPresent()) {
            return leaderboardService.getGlobalLeaderboard(seasonId.get());
        } else {
            return leaderboardService.getGlobalLeaderboardActiveSeason();
        }
    }
}