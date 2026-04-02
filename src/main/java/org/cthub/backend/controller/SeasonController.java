package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.season.SeasonDto;
import org.cthub.backend.service.SeasonService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/season")
public class SeasonController {

    private final SeasonService seasonService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<SeasonDto> getAllSeasons() {
        return seasonService.getAllSeasons();
    }
}
