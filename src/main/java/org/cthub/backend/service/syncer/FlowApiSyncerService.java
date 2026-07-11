package org.cthub.backend.service.syncer;

import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.Season;
import org.cthub.backend.repository.SeasonRepository;
import org.cthub.backend.service.syncer.dto.DrahtVenueDto;
import org.cthub.backend.service.syncer.dto.DrahtVenuesResponseDto;
import org.cthub.backend.service.syncer.dto.FlowEventDto;
import org.cthub.backend.service.syncer.dto.FlowPublicInfoDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FlowApiSyncerService {

    private final RestClient flowClient;
    private final RestClient drahtClient;
    private final FlowApiPersister persister;
    private final SeasonRepository seasonRepository;

    public FlowApiSyncerService(FlowApiPersister persister,
                                SeasonRepository seasonRepository) {
        this.flowClient = RestClient.builder().baseUrl("https://flow.hands-on-technology.org/api").build();
        this.drahtClient = RestClient.builder().baseUrl("https://draht.hands-on-technology.org/custom/handson/api_proxy.php/handson/node/public").build();
        this.persister = persister;
        this.seasonRepository = seasonRepository;
    }

    @Async
    public void syncActiveSeason() {
        log.info("🚀 Starting API Sync for current season...");

        Season activeSeason = seasonRepository.findByActiveTrue()
            .orElseThrow(() -> new IllegalStateException("No active season found in DB for sync."));

        // --- PRE-FETCH: Get all Venues ---
        DrahtVenuesResponseDto venuesResponse = drahtClient.get().uri("/venues").retrieve().body(DrahtVenuesResponseDto.class);
        Map<Integer, DrahtVenueDto> venueMap = (venuesResponse != null && venuesResponse.getData() != null)
            ? venuesResponse.getData().stream().collect(Collectors.toMap(DrahtVenueDto::getId, v -> v, (v1, v2) -> v1))
            : Map.of();

        // --- STEP 1: Get all slugs ---
        Map<String, String> slugMap = flowClient.get().uri("/events/").retrieve().body(new ParameterizedTypeReference<>() {});
        if (slugMap == null || slugMap.isEmpty()) return;

        Set<String> processedActiveSlugs = new HashSet<>();
        int successCount = 0;

        // --- STEP 2 & 3: Process individual events ---
        for (String slug : slugMap.keySet()) {
            if (slug.endsWith("-explore")) continue;

            try {
                FlowEventDto event = flowClient.get()
                    .uri("/events/slug/{slug}", slug)
                    .retrieve()
                    .body(FlowEventDto.class);

                if (event == null || event.getChallengeId() == null) continue;

                if (!Objects.equals(event.getSeasonRel().getYear(), activeSeason.getStartYear())) {
                    log.info("Skipping event {} as it belongs to season {}.", slug, event.getSeasonRel().getYear());
                    continue;
                }

                FlowPublicInfoDto publicInfo = null;
                try {
                    publicInfo = flowClient.get().uri("/publish/public-information/{flowId}", event.getId()).retrieve().body(FlowPublicInfoDto.class);
                } catch (Exception e) {
                    // Sometimes this endpoint fails or returns 404 if teams aren't published.
                    // We catch it here so we can still save the event with Venue data!
                    log.debug("Public info not available yet for {}", event.getName());
                }

                // Look up the country and coordinates from our pre-fetched map
                DrahtVenueDto venueData = venueMap.get(event.getChallengeId());

                boolean isSavedAndActive = persister.upsertEventAndTeams(activeSeason, event, publicInfo, venueData);

                if (isSavedAndActive) {
                    processedActiveSlugs.add(event.getSlug());
                    successCount++;
                }

            } catch (Exception e) {
                log.error("❌ Failed to sync event with slug {}: {}", slug, e.getMessage());
            }
        }
        persister.deactivateMissingEvents(activeSeason, processedActiveSlugs);

        log.info("✅ API Sync Complete. Successfully synced {} events.", successCount);
    }
}