package org.fllhub.backend.controller;

import org.fllhub.backend.service.FllScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class TestController {
    @Autowired
    private FllScraperService scraperService;

    @GetMapping("/api/test")
    public Map<String, String> test() {
        // Spring automatically converts this Map to JSON
        return Map.of(
            "status", "online",
            "message", "Hello FLL Hub! Backend is running."
        );
    }

    @GetMapping("/api/trigger-scrape")
    public String triggerScrape() {
        scraperService.scrapeCurrentSeason();
        return "Scraping started! Check your IntelliJ console.";
    }
}