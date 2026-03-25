package org.cthub.backend.migration;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Season;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.SeasonRepository;
import org.cthub.backend.service.scraper.ScraperService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddOldSeasonsOverviewTask implements CommandLineRunner {

    record SeasonData(
        String id,
        String name,
        Integer startYear,
        String overviewUrl,
        String finalUrlPart,
        Integer maxPoints
    ) {}

    private static final List<SeasonData> SEASONS_TO_ADD = List.of(
        new SeasonData("2022-23", "Superpowered", 2022, "https://web.archive.org/web/20230306064212/https://www.first-lego-league.org/de/austragungsorte", "finale-2022-23---dresden", 410),
        new SeasonData("2023-24", "Masterpiece", 2023, "https://web.archive.org/web/20240416003245/https://www.first-lego-league.org/de/austragungsorte", "finale-2024-25", 550),
        new SeasonData("2024-25", "Submerged", 2024, "https://web.archive.org/web/20250326220633/https://www.first-lego-league.org/de/austragungsorte", "finale-2025-26", 620)
    );

    private final ScraperService scraperService;
    private final SeasonRepository seasonRepository;
    private final CompetitionRepository competitionRepository;

    @Override
    public void run(String... args) throws Exception {
        for(SeasonData season : SEASONS_TO_ADD) {
            log.info("Adding season: {} ({})", season.name, season.id);

            if(seasonRepository.existsById(season.id)) {
                log.info("Season {} already exists. Skipping.", season.id);
                continue;
            }

            Season newSeason = Season.builder()
                .id(season.id)
                .name(season.name)
                .startYear(season.startYear)
                .active(false)
                .maxPoints(season.maxPoints)
                .overviewHash(null) // Will be set after scraping
                .build();

            seasonRepository.save(newSeason);


            List<Competition> comps = scraperService.syncOverview(newSeason, true, season.overviewUrl);

            Competition finalComp = comps.stream()
                .filter(c -> c.getType().equals(Competition.CompetitionType.FINAL)).findFirst().orElseThrow(() -> new RuntimeException("Final competition not found for season " + season.id));

            finalComp.setUrlPart(season.finalUrlPart);

            competitionRepository.save(finalComp);

            log.info("Season {} added with {} competitions.", season.id, comps.size());
        }
    }
}


