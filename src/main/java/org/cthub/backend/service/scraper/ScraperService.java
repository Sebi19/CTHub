package org.cthub.backend.service.scraper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Season;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.SeasonRepository;
import org.cthub.backend.service.scraper.dto.*;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperService {

    private static final double MAX_ALLOWED_COMPETITION_DROP = 0.20; // Max 20% drop allowed
    private static final int MIN_COMPETITIONS_FOR_SANITY = 10; // Only enforce ratio if we have >10 events

    private final FllHtmlParser parser;
    private final ScraperPersister persister;
    private final SeasonRepository seasonRepository;
    private final CompetitionRepository competitionRepository;

    private static final String LOCATIONS_URL = "https://www.first-lego-league.org/de/austragungsorte";
    private static final String ROBOT_GAME_RESULTS_BASE_URL = "https://evaluation.hands-on-technology.org/de/rg-score/";
    private static final String AWARDS_RESULTS_BASE_URL = "https://evaluation.hands-on-technology.org/de/score/";

    // ==========================================
    // 1. FULL SYNC (Nightly) 🌙
    // ==========================================

    @Async
    public void runFullSync(boolean ignoreHashes) {
        log.info("🌒 STARTING FULL SYNC...");
        long start = System.currentTimeMillis();

        // 1. Get Active Season (or create default)
        Season season = seasonRepository.findByActiveTrue()
            .orElseGet(() -> seasonRepository.save(Season.builder()
                .id("2025-26") // Adjust logic as needed
                .name("Unearthed")
                .startYear(2025)
                .active(true)
                .build()));

        // 2. Sync The Overview List
        List<Competition> activeCompetitions = syncOverview(season, ignoreHashes);

        // 3. Sync Details for Each Competition
        int updatedCount = 0;
        for (Competition comp : activeCompetitions) {
            /*try {*/
                if (processSingleCompetition(comp, ignoreHashes)) {
                    updatedCount++;
                }
            /*} catch (Exception e) {
                log.error("❌ Failed to process competition '{}': {}", comp.getName(), e.getMessage());
                // Continue loop - don't let one failure stop the night
            }*/
        }

        long duration = (System.currentTimeMillis() - start) / 1000;
        log.info("✅ FULL SYNC COMPLETE in {}s. Updated {} competitions.", duration, updatedCount);
    }

    // ==========================================
    // 2. QUICK SYNC (Hourly) ⚡
    // ==========================================

    @Async
    public void runQuickResultSync() {
        log.info("⚡ STARTING QUICK RESULT SYNC...");

        // Find competitions that happened (or are happening) but have no results yet
        List<Competition> pending = competitionRepository.findPendingResults(LocalDate.now());
        int foundCount = 0;

        log.info("⚡ Found {} competitions without results. Checking for updates...", pending.size());

        for (Competition comp : pending) {
            try {
                // Strategy: Only fetch if we find a valid result page
                boolean foundNewResults = false;

                // Case A: We already have a URL (from Detail page)
                if (comp.getResultsUrlPart() != null && !comp.getResultsUrlPart().isEmpty()) {
                    foundNewResults = checkAndScrapeResults(comp, comp.getResultsUrlPart());
                }
                // Case B: No URL yet? GUESS IT!
                else {
                    List<String> guesses = generatePossibleResultUrlParts(comp.getSeason(), comp.getUrlPart());
                    for (String guess : guesses) {
                        if (checkAndScrapeResults(comp, guess)) {
                            foundNewResults = true;
                            break; // Stop guessing
                        }
                    }
                }

                if (foundNewResults) foundCount++;

            } catch (Exception e) {
                log.error("Failed quick sync for {}: {}", comp.getName(), e.getMessage());
            }
        }
        log.info("⚡ QUICK SYNC DONE. Found new results for {} events.", foundCount);
    }

    // ==========================================
    // 3. CORE LOGIC (The "Brain") 🧠
    // ==========================================

    /**
     * @return true if any data was updated in the DB
     */
    private boolean processSingleCompetition(Competition comp, boolean ignoreHashes) {
        log.info("🔍 Processing competition: {}", comp.getName());
        boolean dataChanged = false;

        // --- STEP 1: DETAILS & TEAMS ---
        String detailHtml = fetchOrNull(generateDetailUrl(comp));
        ScrapedEventDetailsDto detailsDto = null;

        if (detailHtml != null) {
            String newDetailHash = parser.computeDetailHash(detailHtml);

            // HASH CHECK 🛑
            if (ignoreHashes || !newDetailHash.equals(comp.getDetailHash())) {
                log.info("📝 Parsing Details for: {}", comp.getName());
                ScrapedEventDetailsDto candidateDto = parser.parseEventPage(detailHtml);

                boolean suspiciousParsing = candidateDto.getTeams().isEmpty() && comp.getRegisteredTeamCount() > 0;

                if (suspiciousParsing) {
                    log.error("⛔ SANITY CHECK FAILED for {}: Overview says {} teams, but parsed 0. Skipping update.",
                        comp.getName(), comp.getRegisteredTeamCount());
                    // We DO NOT set dataChanged = true. We silently skip this update.
                } else {
                    // All good, accept the data
                    log.info("📝 Parsing Details for: {}", comp.getName());
                    detailsDto = candidateDto;
                    detailsDto.setContentHash(newDetailHash);
                    dataChanged = true;
                }
            }
        }

        // --- STEP 2: RESULTS (Robot Game & Awards) ---
        ScrapedRobotGameResultsDto robotGameDto = null;
        ScrapedAwardsAndRanksDto awardsDto = null;

        // Determine Results URL: Use the one from DTO (freshly discovered) or fallback to DB
        String resultsUrlPart = (detailsDto != null && detailsDto.getResultsUrlPart() != null)
            ? detailsDto.getResultsUrlPart()
            : comp.getResultsUrlPart();

        if (resultsUrlPart != null) {
            // A. Robot Game
            String robotGameResultsHtml = fetchOrNull(ROBOT_GAME_RESULTS_BASE_URL + resultsUrlPart);

            if (robotGameResultsHtml != null) {
                String newRgHash = parser.computeRobotGameHash(robotGameResultsHtml);
                if (!newRgHash.isEmpty() && (ignoreHashes || !newRgHash.equals(comp.getRobotGameHash()))) {
                    log.info("🤖 Parsing Scores for: {}", comp.getName());
                    robotGameDto = parser.parseRobotGameResults(robotGameResultsHtml);
                    robotGameDto.setContentHash(newRgHash);
                    dataChanged = true;
                }
            }

            // B. Awards
            String awardsResultsHtml = fetchOrNull(AWARDS_RESULTS_BASE_URL + resultsUrlPart);

            if (awardsResultsHtml != null) {
                String newAwardsHash = parser.computeAwardsHash(awardsResultsHtml);
                if (!newAwardsHash.isEmpty() && (ignoreHashes || !newAwardsHash.equals(comp.getAwardsHash()))) {
                    log.info("🏆 Parsing Awards for: {}", comp.getName());
                    awardsDto = parser.parseAwardResults(awardsResultsHtml);
                    awardsDto.setContentHash(newAwardsHash);
                    dataChanged = true;
                }
            }
        }

        // --- STEP 3: PERSIST (Unified Transaction) ---
        if (dataChanged) {
            log.info("💾 Persisting updates for: {}", comp.getName());
            persister.updateCompetition(comp, detailsDto, robotGameDto, awardsDto);
            return true;
        }
        return false;
    }

    private List<Competition> syncOverview(Season season, boolean ignoreHashes) {
        String html = fetchOrNull(LOCATIONS_URL);
        if (html == null) return new ArrayList<>();

        String newHash = parser.computeOverviewHash(html);

        // HASH CHECK 🛑
        if (!ignoreHashes && newHash.equals(season.getOverviewHash())) {
            log.info("🌍 Overview Page Unchanged. Skipping list parse.");
            return competitionRepository.findAllBySeason(season);
        }

        log.info("🌍 Overview Changed! syncing competition list...");
        List<ScrapedEventOverviewDto> dtos = parser.parseCompetitionsList(html, season);

        long currentCount = competitionRepository.countBySeasonAndActiveTrue(season);
        if (currentCount > MIN_COMPETITIONS_FOR_SANITY) {
            // If we parse significantly fewer competitions than we have active in DB
            if (dtos.size() < currentCount * (1.0 - MAX_ALLOWED_COMPETITION_DROP)) {
                log.error("⛔ SANITY CHECK FAILED: Scraper found {} events, but DB has {}. Drop > 20%. ABORTING SYNC.",
                    dtos.size(), currentCount);
                // Return existing to prevent damage, effectively cancelling the update
                return competitionRepository.findAllBySeason(season);
            }
        }

        List<Competition> active = persister.syncCompetitionsFromOverview(season, dtos);

        // Update Season Hash
        season.setOverviewHash(newHash);
        seasonRepository.save(season);

        return active;
    }

    private boolean checkAndScrapeResults(Competition comp, String urlPart) {
        String fullUrl = AWARDS_RESULTS_BASE_URL + urlPart;

        // 1. Check if page exists (Lightweight)
        // Note: You can optimize this with a HEAD request if needed
        String html = fetchOrNull(fullUrl);

        if (html != null && !parser.computeAwardsHash(html).isEmpty()) { // Add this helper to Parser!
            log.info("🎉 Found new results for {} at {}", comp.getName(), urlPart);

            // 2. If valid, trigger the full parse logic for this competition
            // We temporarily set the URL part on the object so processSingleCompetition uses it
            comp.setResultsUrlPart(urlPart);
            processSingleCompetition(comp, false);
            return true;
        }
        return false;
    }

    // ==========================================
    // 4. HELPERS 🛠️
    // ==========================================

    private String fetchOrNull(String url) {
        try {
            return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) ChallengeTeamHub/1.0")
                .timeout(30000)
                .get()
                .outerHtml();
        } catch (IOException e) {
            // Log warning but return null (don't crash the scraper)
            log.warn("⚠️ Could not fetch {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String generateDetailUrl(Competition comp) {
        String seasonPart = getSeasonUrlPart(comp.getSeason());
        return "https://www.first-lego-league.org/de/" + seasonPart + "/" + comp.getUrlPart();
    }

    private String getSeasonUrlPart(Season season) {
        int startYear = season.getStartYear();
        return String.format("challenge-%d-%02d", startYear, (startYear + 1) % 100);
    }

    private List<String> generatePossibleResultUrlParts(Season season, String eventUrlPart) {
        String fullUrl = getSeasonUrlPart(season) + "-" + eventUrlPart;
        List<String> candidates = new ArrayList<>();
        candidates.add(fullUrl); // Standard

        // Lazy ASCII variant (schwaebisch -> schwabisch)
        if (fullUrl.contains("ae") || fullUrl.contains("ue") || fullUrl.contains("oe")) {
            candidates.add(fullUrl
                .replace("ae", "a")
                .replace("ue", "u")
                .replace("oe", "o"));
        }
        return candidates;
    }
}