package org.cthub.backend.service.scraper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.*;
import org.cthub.backend.repository.*;
import org.cthub.backend.service.scraper.dto.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScraperPersister {

    private final CompetitionRepository competitionRepo;
    private final SeasonTeamRepository seasonTeamRepo;
    private final RobotGameResultRepository resultRepo;
    private final PlaceRepository placeRepo;
    private final NominationRepository nominationRepo;

    // ==========================================
    // 1. PHASE 1: OVERVIEW SYNC (The "Master List") 🌍
    // ==========================================

    /**
     * Syncs the list of competitions from the overview page.
     * - Creates new competitions.
     * - Updates existing ones (name, team counts).
     * - DEACTIVATES competitions that disappeared from the website.
     * Returns the list of active/updated competitions to be used for detail scraping.
     */
    @Transactional
    public List<Competition> syncCompetitionsFromOverview(Season season, List<ScrapedEventOverviewDto> dtos) {
        // 1. Fetch ALL existing competitions for this season (Map by URL Part)
        Map<String, Competition> existingMap = competitionRepo.findAllBySeason(season)
            .stream()
            .collect(Collectors.toMap(Competition::getUrlPart, Function.identity()));

        List<Competition> activeCompetitions = new ArrayList<>();
        Set<String> processedUrlParts = new HashSet<>();

        // 2. Process the Web List (Upsert)
        for (ScrapedEventOverviewDto dto : dtos) {
            String urlPart = dto.getUrlPart();
            processedUrlParts.add(urlPart);

            Competition comp = existingMap.getOrDefault(urlPart, Competition.builder()
                .season(season)
                .urlPart(urlPart)
                .build());

            // Update Fields from Overview
            comp.setName(dto.getName());
            comp.setCountry(dto.getCountry());
            comp.setType(dto.getType());
            comp.setRegisteredTeamCount(dto.getRegisteredTeamCount());
            comp.setMaxTeamCount(dto.getMaxTeamCount());

            // Mark as Active (in case it was previously inactive)
            comp.setActive(true);

            activeCompetitions.add(comp);
        }

        // 3. Process the "Disappeared" (Soft Delete)
        // Any competition in DB that was NOT in the DTO list gets deactivated.
        for (Competition existing : existingMap.values()) {
            if (!processedUrlParts.contains(existing.getUrlPart())) {
                if (existing.isActive()) {
                    log.info("📉 Competition disappeared: {} (Setting inactive)", existing.getName());
                    existing.setActive(false);
                }
                // We don't add it to activeCompetitions, so we won't scrape its details.
            }
        }

        // 4. Batch Save Everything (Active updates + Inactive updates)
        List<Competition> allToSave = new ArrayList<>(activeCompetitions);
        allToSave.addAll(existingMap.values()); // This includes the inactive ones we just updated

        // Use saveAll to flush changes
        competitionRepo.saveAll(allToSave);

        return activeCompetitions;
    }

    /**
     * THE MASTER UPDATE METHOD 🛠️
     * Handles Metadata, Teams, Scores, and Awards in ONE transaction.
     * * @param comp The competition to update
     * @param details Optional: Present if Detail Hash changed
     * @param robotGameResults Optional: Present if Results Hash changed
     * @param nominations Optional: Present if Awards Hash changed
     */
    @Transactional
    public void updateCompetition(Competition comp,
                                  ScrapedEventDetailsDto details,
                                  ScrapedRobotGameResultsDto robotGameResults,
                                  ScrapedAwardsAndRanksDto nominations) {

        // 1. SYNC TEAMS (If needed) 👥
        // We need the Team Map for scores/awards later, so we must either
        // sync (and get fresh map) or just fetch the existing map.
        Map<String, SeasonTeam> teamMap;

        if (details != null) {
            // A. Update Metadata
            updateCompetitionMetadata(comp, details);
            comp.setDetailHash(details.getContentHash());

            // B. Sync Teams (Create/Update/Delete) & Get Fresh Map
            teamMap = syncTeamsAndReturnMap(comp, details.getTeams());
        } else {
            // C. No Team changes -> Just fetch existing map for lookups
            teamMap = seasonTeamRepo.findByRegisteredCompetitionsContains(comp)
                .stream()
                .collect(Collectors.toMap(SeasonTeam::getFllId, Function.identity()));
        }

        // 2. SYNC SCORES (If needed) 🤖
        if (robotGameResults != null) {
            syncScores(comp, robotGameResults, teamMap);
            comp.setRobotGameHash(robotGameResults.getContentHash());
            comp.setResultsAvailable(true);
        }

        // 3. SYNC NOMINATIONS (If needed) 🏆
        if (nominations != null) {
            syncNominations(comp, nominations, teamMap);
            comp.setAwardsHash(nominations.getContentHash());
            comp.setResultsAvailable(true);
        }

        // 4. SAVE COMPETITION (Updates Hashes & Metadata)
        // The hashes should be set on the 'comp' object by the Service BEFORE calling this method.
        competitionRepo.save(comp);
    }

    // --- Helpers ---

    private void updateCompetitionMetadata(Competition comp, ScrapedEventDetailsDto details) {
        comp.setDate(details.getDate());
        comp.setLocation(details.getLocation());
        comp.setQualificationUrlPart(details.getQualificationUrlPart());

        Competition.ContactInfo contact = new Competition.ContactInfo();
        contact.setContactName(details.getContactName());
        contact.setContactEmail(details.getContactEmail());
        comp.setContact(contact);

        if (details.getWebLinks() != null) {
            comp.setLinks(mapLinks(details.getWebLinks()));
        }

        if (details.getResultsUrlPart() != null) {
            comp.setResultsUrlPart(details.getResultsUrlPart());
        }
    }

    /**
     * Returns a Map of <FllId, SeasonTeam> representing the CURRENT state of the DB
     */
    private Map<String, SeasonTeam> syncTeamsAndReturnMap(Competition comp, List<ScrapedTeamDto> scrapedTeams) {
        // 1. Fetch Existing (Map by FLL ID)
        Map<String, SeasonTeam> dbTeamsMap = seasonTeamRepo.findByRegisteredCompetitionsContains(comp)
            .stream()
            .collect(Collectors.toMap(SeasonTeam::getFllId, Function.identity()));

        Set<String> scrapedIds = new HashSet<>();
        List<SeasonTeam> teamsToSave = new ArrayList<>();
        List<ScrapedTeamDto> newCandidates = new ArrayList<>();

        // 2. Identification (Update vs New)
        for (ScrapedTeamDto scraped : scrapedTeams) {
            String fllId = scraped.getFllId();
            scrapedIds.add(fllId);

            if (dbTeamsMap.containsKey(fllId)) {
                // UPDATE
                SeasonTeam existing = dbTeamsMap.get(fllId);
                updateTeamFields(existing, scraped);
                teamsToSave.add(existing);
            } else {
                // NEW CANDIDATE
                newCandidates.add(scraped);
            }
        }

        // 3. Handle New Candidates (Global Lookup)
        if (!newCandidates.isEmpty()) {
            List<String> newIds = newCandidates.stream().map(ScrapedTeamDto::getFllId).toList();
            Map<String, SeasonTeam> seasonGlobalMap = seasonTeamRepo.findBySeasonIdAndFllIdIn(comp.getSeason().getId(), newIds)
                .stream()
                .collect(Collectors.toMap(SeasonTeam::getFllId, Function.identity()));

            for (ScrapedTeamDto candidate : newCandidates) {
                SeasonTeam team = seasonGlobalMap.get(candidate.getFllId());

                if (team == null) {
                    // Create NEW Global Team
                    team = SeasonTeam.builder()
                        .season(comp.getSeason())
                        .fllId(candidate.getFllId())
                        .registeredCompetitions(new HashSet<>())
                        .active(true) // Default active
                        .build();
                } else {
                    // Reactivate if it was inactive (it just joined a comp!)
                    team.setActive(true);
                }

                if (team.getRegisteredCompetitions() == null) {
                    team.setRegisteredCompetitions(new HashSet<>());
                }
                team.getRegisteredCompetitions().add(comp);

                updateTeamFields(team, candidate);
                teamsToSave.add(team);
            }
        }

        // 4. Handle Deletions (The "Ghost" Cleanup) 👻
        for (String dbId : dbTeamsMap.keySet()) {
            if (!scrapedIds.contains(dbId)) {
                SeasonTeam ghost = dbTeamsMap.get(dbId);

                // A. Unlink from THIS competition
                ghost.getRegisteredCompetitions().remove(comp);

                // B. Check if they are now homeless (orphaned)
                if (ghost.getRegisteredCompetitions().isEmpty()) {
                    log.info("👻 Team {} has no competitions left. Setting inactive.", ghost.getFllId());
                    ghost.setActive(false);
                }

                teamsToSave.add(ghost);
            }
        }

        seasonTeamRepo.saveAll(teamsToSave);

        return seasonTeamRepo.findByRegisteredCompetitionsContains(comp)
            .stream()
            .collect(Collectors.toMap(SeasonTeam::getFllId, Function.identity()));
    }

    private void syncScores(Competition comp, ScrapedRobotGameResultsDto robotGameResults, Map<String, SeasonTeam> teamMap) {
        resultRepo.deleteByCompetition(comp);
        resultRepo.flush();

        List<RobotGameResult> results = new ArrayList<>();
        for (ScrapedRobotGameScoreDto dto : robotGameResults.getScores()) {
            SeasonTeam team = teamMap.get(dto.getFllId());
            if (team != null) {
                results.add(RobotGameResult.builder().competition(comp).seasonTeam(team)
                    .pr1(dto.getRun1()).pr2(dto.getRun2()).pr3(dto.getRun3()).bestPr(dto.getBestRun())
                    .qf(dto.getQuarterFinal()).sf(dto.getSemiFinal()).f1(dto.getFinal1()).f2(dto.getFinal2())
                    .rank(dto.getRank())
                    .build());
            }
        }
        resultRepo.saveAll(results);
    }

    private void syncNominations(Competition comp, ScrapedAwardsAndRanksDto data, Map<String, SeasonTeam> teamMap) {
        // Name Lookup Helper
        Map<String, SeasonTeam> nameMap = teamMap.values().stream()
            .collect(Collectors.toMap(t -> t.getName().toLowerCase().trim(), Function.identity(), (a, b) -> a));

        // Delete Old
        placeRepo.deleteByCompetition(comp);
        placeRepo.flush();
        nominationRepo.deleteByCompetition(comp);
        nominationRepo.flush();

        // Save Places
        List<Place> places = new ArrayList<>();
        for (ScrapedPlaceDto dto : data.getPlaces()) {
            SeasonTeam team = nameMap.get(dto.getTeamName().toLowerCase().trim());
            if (team != null) {
                places.add(Place.builder().competition(comp).seasonTeam(team)
                    .place(dto.getPlace()).advancing(dto.isAdvancing()).build());
            }
        }
        placeRepo.saveAll(places);

        // Save Nominations
        List<Nomination> nominations = new ArrayList<>();
        for (ScrapedNominationDto dto : data.getNominations()) {
            SeasonTeam team = nameMap.get(dto.getTeamName().toLowerCase().trim());
            if (team != null) {
                nominations.add(Nomination.builder().competition(comp).seasonTeam(team)
                    .category(dto.getCategory()).isAwardWinner(dto.isWinner()).build());
            }
        }
        nominationRepo.saveAll(nominations);
    }

    private void updateTeamFields(SeasonTeam team, ScrapedTeamDto dto) {
        team.setName(dto.getName());
        team.setInstitution(dto.getInstitution());
        team.setCity(dto.getCity());
        if (dto.getLinks() != null) {
            team.setLinks(mapLinks(dto.getLinks()));
        }
    }

    private List<Link> mapLinks(List<ScrapedLinkDto> dtos) {
        return dtos.stream()
            .map(d -> Link.builder().label(d.getLabel()).url(d.getUrl()).build())
            .collect(Collectors.toList());
    }
}