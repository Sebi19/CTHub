package org.cthub.backend.service.scraper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScraperScheduler {

    private final ScraperService scraperService;

    // ==========================================
    // 1. NIGHTLY FULL SYNC 🌙
    // ==========================================
    // Runs every day at 03:00 AM
    // This syncs everything: Teams, Details, Scores, Awards.
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Vienna")
    public void scheduleFullSync() {
        log.info("⏰ Scheduler triggered: Nightly Full Sync");
        scraperService.runFullSync();
    }

    // ==========================================
    // 2. HOURLY QUICK SYNC ⚡
    // ==========================================
    // Runs every hour from 06:00 to 23:00 (6 AM to 11 PM)
    // Only checks for new results on past/today's events.
    @Scheduled(cron = "0 0 6-23 * * *", zone = "Europe/Vienna")
    public void scheduleQuickSync() {
        log.info("⏰ Scheduler triggered: Hourly Quick Sync");
        scraperService.runQuickResultSync();
    }
}