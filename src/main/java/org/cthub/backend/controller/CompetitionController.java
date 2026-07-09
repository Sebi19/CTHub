package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.competition.CompetitionDetailDto;
import org.cthub.backend.dto.competition.CompetitionShortInfoDto;
import org.cthub.backend.service.CompetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/seasons/{seasonId}/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping("/{urlPart}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<CompetitionDetailDto> getCompetitionDetails(
        @PathVariable String seasonId,
        @PathVariable String urlPart) {

        CompetitionDetailDto detailDto = competitionService.getCompetitionDetails(seasonId, urlPart);

        return ResponseEntity.ok(detailDto);
    }

    @GetMapping()
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<CompetitionShortInfoDto>> getCompetitionsForSeason(
        @PathVariable String seasonId) {

        List<CompetitionShortInfoDto> competitions = competitionService.getCompetitionsForSeason(seasonId);

        return ResponseEntity.ok(competitions);
    }
}