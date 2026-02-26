package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.competition.CompetitionDetailDto;
import org.cthub.backend.service.CompetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/seasons/{seasonId}/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping("/{urlPart}")
    public ResponseEntity<CompetitionDetailDto> getCompetitionDetails(
        @PathVariable String seasonId,
        @PathVariable String urlPart) {

        CompetitionDetailDto detailDto = competitionService.getCompetitionDetails(seasonId, urlPart);

        return ResponseEntity.ok(detailDto);
    }
}