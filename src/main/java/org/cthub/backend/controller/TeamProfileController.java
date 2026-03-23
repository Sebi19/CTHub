package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.team.TeamProfileDetailsDto;
import org.cthub.backend.service.TeamProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/profiles")
public class TeamProfileController {

    private final TeamProfileService teamProfileService;

    @GetMapping("/{profileUrl}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TeamProfileDetailsDto> getTeamProfileDetails(
        @PathVariable String profileUrl) {
        TeamProfileDetailsDto detailDto = teamProfileService.getTeamProfileDetails(profileUrl);

        return ResponseEntity.ok(detailDto);
    }
}