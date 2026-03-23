package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.team.SeasonTeamDetailsDto;
import org.cthub.backend.service.SeasonTeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/seasons/{seasonId}/teams")
public class SeasonTeamController {

    private final SeasonTeamService seasonTeamService;

    @GetMapping("/{fllId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SeasonTeamDetailsDto> getTeamDetails(
        @PathVariable String seasonId,
        @PathVariable String fllId) {

        SeasonTeamDetailsDto detailDto = seasonTeamService.getSeasonTeamDetails(seasonId, fllId);

        return ResponseEntity.ok(detailDto);
    }
}