package org.cthub.backend.service.syncer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"prod", "dev"}) // Enabled for both production and development environments
public class SyncerScheduler {

    private final FlowApiSyncerService syncerService;

    // ==========================================
    // HOURLY API SYNC ⚡
    // ==========================================
    // Runs at the top of every hour (e.g., 08:00, 09:00, 10:00).
    // Since the API sync is lightning-fast and bypasses DOM scraping,
    // it is completely safe to run this frequently to keep team counts and metadata fresh.
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Vienna")
    public void scheduleActiveSeasonSync() {
        log.info("⏰ Scheduler triggered: Hourly API Sync for the active season");
        syncerService.syncActiveSeason();
    }
}