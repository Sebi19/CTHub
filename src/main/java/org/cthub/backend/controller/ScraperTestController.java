package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.service.scraper.ScraperService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ScraperTestController {

    private final ScraperService scraperService;

    @Value("${scraper.admin-pw:changeMe!}")
    private String adminPassword;

    @GetMapping("/api/test")
    public Map<String, String> test() {
        // Spring automatically converts this Map to JSON
        return Map.of(
            "status", "online",
            "message", "Hello FLL Hub! Backend is running."
        );
    }

    @GetMapping("/api/force-full")
    public ResponseEntity<String> forceFullSync(@RequestParam String pw) {
        if (!adminPassword.equals(pw)) {
            log.warn("⛔ Unauthorized attempt to trigger Full Sync");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong password");
        }

        // Run in background
        scraperService.runFullSync();
        return ResponseEntity.ok("🌙 Full Sync started in background.");
    }

    @GetMapping("/api/force-quick")
    public ResponseEntity<String> forceQuickSync(@RequestParam String pw) {
        if (!adminPassword.equals(pw)) {
            log.warn("⛔ Unauthorized attempt to trigger Quick Sync");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong password");
        }

        scraperService.runQuickResultSync();
        return ResponseEntity.ok("⚡ Quick Sync started in background.");
    }
}