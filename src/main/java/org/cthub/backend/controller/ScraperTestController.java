package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.service.scraper.ScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/scraper")
public class ScraperTestController {

    private final ScraperService scraperService;

    @GetMapping("/test")
    public Map<String, String> test() {
        // Spring automatically converts this Map to JSON
        return Map.of(
            "status", "online",
            "message", "Hello FLL Hub! Backend is running."
        );
    }

    @GetMapping("/force-full")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceFullSync(
        @RequestParam(defaultValue = "false") boolean ignoreHashes
    ) {
        // Run in background
        scraperService.runFullSync(ignoreHashes);
        return ResponseEntity.ok("🌙 Full Sync started in background.");
    }

    @GetMapping("/force-quick")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceQuickSync() {
        scraperService.runQuickResultSync();
        return ResponseEntity.ok("⚡ Quick Sync started in background.");
    }

    @GetMapping("/sync-competition/{seasonId}/{urlPart}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> syncCompetition(
        @PathVariable String seasonId,
        @PathVariable String urlPart,
        @RequestParam(defaultValue = "false") boolean ignoreHashes
    ) {
        scraperService.syncSingleCompetition(seasonId, urlPart, ignoreHashes);
        return ResponseEntity.ok("🔄 Syncing competition " + seasonId + "/" + urlPart + " started in background.");
    }

    @GetMapping("/fetch-old/{seasonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> fetchOldSeasonData(
        @PathVariable String seasonId,
        @RequestParam(defaultValue = "false") boolean ignoreHashes,
        @RequestParam(defaultValue = "false") boolean skipWithHashes
    ) {
        scraperService.fetchOldSeasonData(seasonId, ignoreHashes, skipWithHashes);
        return ResponseEntity.ok("📦 Fetching old season data for " + seasonId + " started in background.");
    }
}