package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.search.SearchResultItemDto;
import org.cthub.backend.service.SearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public List<SearchResultItemDto> globalSearch(
        @RequestParam("q") String query,
        @RequestParam(value = "seasonId", required = false) String seasonId) {

        // Prevent database strain from empty or 1-letter searches
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        return searchService.performGlobalSearch(query, seasonId);
    }
}
