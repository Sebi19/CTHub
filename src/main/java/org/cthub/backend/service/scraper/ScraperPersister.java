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

    private final CompetitionRepository competitionRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final RobotGameResultRepository robotGameResultRepository;
    private final PlaceRepository placeRepository;
    private final NominationRepository nominationRepository;

    private record HistoricalTeam (String seasonId, String fllId, String name, String institution, String city) {}

    private record NameChange(String seasonId, String fllId, String oldName) {}

    private final List<HistoricalTeam> historicalTeams = List.of(
        new HistoricalTeam("2023-24", "1626", "Wilhelm-Robotics", "Wilhelm-Gymnasium", "Braunschweig"),
        new HistoricalTeam("2023-24", "1486", "Robobriks", "Schulzentrum am Sund", "Stralsund")
    );

    private final List<NameChange> nameChanges = List.of(
        new NameChange("2024-25", "1692", "Sponge Bots"),
        new NameChange("2024-25", "1356", "Festo2"),
        new NameChange("2024-25", "1357", "Festo3"),

        new NameChange("2023-24", "1745", "Roboterfreunde Kornelimünster e.V."),
        new NameChange("2023-24", "1745", "Roboterfreunde Kornelimünster"),
        new NameChange("2023-24", "1439", "MPDVeruEckTen"),
        new NameChange("2023-24", "1474", "RoDotties"),
        new NameChange("2023-24", "1113", "RoboWip"),
        new NameChange("2023-24", "1218", "CaroAces"),
        new NameChange("2023-24", "1305", "Future Stars"),
        new NameChange("2023-24", "1308", "Ninja"),
        new NameChange("2023-24", "1131", "SCUOLA DI ROBOTICA LUGANO - MASSAGNO 1"),
        new NameChange("2023-24", "1132", "SCUOLA DI ROBOTICA LUGANO - ELVETICO 1"),
        new NameChange("2023-24", "1133", "SCUOLA DI ROBOTICA LUGANO - ELVETICO 2"),
        new NameChange("2023-24", "1134", "SCUOLA DI ROBOTICA LUGANO - MASSAGNO 2"),
        new NameChange("2023-24", "1262", "GN-Creators"),
        new NameChange("2023-24", "1518", "Integrastic Robots"),

        new NameChange("2022-23", "1569", "NaWiKlasse6a(1)")
    );

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
        Map<String, Competition> existingMap = competitionRepository.findAllBySeason(season)
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
                .resultsAvailable(false)
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
        competitionRepository.saveAll(allToSave);

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
            teamMap = seasonTeamRepository.findByRegisteredCompetitionsContains(comp)
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
        competitionRepository.save(comp);
    }

    // --- Helpers ---

    private void updateCompetitionMetadata(Competition comp, ScrapedEventDetailsDto details) {
        comp.setDate(details.getDate());
        comp.setEndDate(details.getEndDate());
        comp.setLocation(details.getLocation());
        if (details.getQualificationUrlPart() != null) {
            comp.setQualificationUrlPart(details.getQualificationUrlPart());
        }

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
        Map<String, SeasonTeam> dbTeamsMap = seasonTeamRepository.findByRegisteredCompetitionsContains(comp)
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
            Map<String, SeasonTeam> seasonGlobalMap = seasonTeamRepository.findBySeasonIdAndFllIdIn(comp.getSeason().getId(), newIds)
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

        seasonTeamRepository.saveAll(teamsToSave);

        return seasonTeamRepository.findByRegisteredCompetitionsContains(comp)
            .stream()
            .collect(Collectors.toMap(SeasonTeam::getFllId, Function.identity()));
    }

    private SeasonTeam resolveSeasonTeamById(Competition comp, Map<String, SeasonTeam> teamMap, String fllId) {
        SeasonTeam team = teamMap.get(fllId);
        if (team != null) {
            return team;
        }

        Optional<SeasonTeam> newTeam = seasonTeamRepository.findBySeasonIdAndFllIdWithDetails(comp.getSeason().getId(), fllId);
        if (newTeam.isPresent()) {
            team = newTeam.get();
            log.info("🔍 Found new team for FLL ID '{}'. Adding to competition.", fllId);
            teamMap.put(fllId, team);
            team.getRegisteredCompetitions().add(comp); // Link the team to the competition
            seasonTeamRepository.save(team); // Save the new association
            return team;
        }

        HistoricalTeam historicalTeam = historicalTeams.stream()
            .filter(ht -> ht.seasonId().equals(comp.getSeason().getId()) && ht.fllId().equals(fllId))
            .findFirst()
            .orElse(null);
        if (historicalTeam != null) {
            team = SeasonTeam.builder()
                .season(comp.getSeason())
                .fllId(historicalTeam.fllId())
                .name(historicalTeam.name())
                .institution(historicalTeam.institution())
                .city(historicalTeam.city())
                .registeredCompetitions(new HashSet<>(Set.of(comp)))
                .active(false)
                .build();
            teamMap.put(fllId, team);
            seasonTeamRepository.save(team);
            log.info("📜 Created historical team for FLL ID '{}': {} ({}). Marked as inactive.", fllId, historicalTeam.name(), historicalTeam.institution());
            return team;
        }

        log.warn("Could not find team for FLL ID '{}'. Skipping.", fllId);
        return null;
    }

    private SeasonTeam resolveSeasonTeamByName(Competition comp, Map<String, SeasonTeam> teamMap, String teamName) {
        // First try direct name match
        Optional<SeasonTeam> directMatch = teamMap.values().stream()
            .filter(t -> t.getName().trim().equalsIgnoreCase(teamName.trim()))
            .findFirst();

        if (directMatch.isPresent()) {
            return directMatch.get();
        }

        // If no direct match, check for name changes
        Optional<NameChange> nameChange = nameChanges.stream()
            .filter(nc -> nc.seasonId().equals(comp.getSeason().getId()) && nc.oldName().trim().equalsIgnoreCase(teamName.trim()))
            .findFirst();

        if (nameChange.isPresent()) {
            String fllId = nameChange.get().fllId();
            SeasonTeam team = resolveSeasonTeamById(comp, teamMap, fllId);
            if (team != null && teamName.isBlank()) {
                team.setName(nameChange.get().oldName());
                log.info("⚠️ Team with FLL ID '{}' has blank name. Using old name from name change record: '{}'", team.getFllId(), team.getName());
                seasonTeamRepository.save(team);
            }
            return team;
        }

        log.warn("Could not find team for name '{}'. No direct match or name change found.", teamName);
        return null;
    }

    private void syncScores(Competition comp, ScrapedRobotGameResultsDto robotGameResults, Map<String, SeasonTeam> teamMap) {
        robotGameResultRepository.deleteByCompetition(comp);
        robotGameResultRepository.flush();

        List<RobotGameResult> results = new ArrayList<>();
        List<ProcessedRobotGameScoreDto> processedScores = calculatePrelimRanks(robotGameResults.getScores());
        for (ProcessedRobotGameScoreDto processedDto : processedScores) {
            ScrapedRobotGameScoreDto dto = processedDto.getRobotGameScoreDto();
            SeasonTeam team = resolveSeasonTeamById(comp, teamMap, dto.getFllId());
            if (team != null) {
                results.add(RobotGameResult.builder().competition(comp).seasonTeam(team)
                    .pr1(dto.getRun1()).pr2(dto.getRun2()).pr3(dto.getRun3()).bestPr(dto.getBestRun())
                    .qf(dto.getQuarterFinal()).sf(dto.getSemiFinal()).f1(dto.getFinal1()).f2(dto.getFinal2())
                    .rank(dto.getRank()).prelimRank(processedDto.getPrelimRank())
                    .build());
            }
        }
        robotGameResultRepository.saveAll(results);
    }

    private List<ProcessedRobotGameScoreDto> calculatePrelimRanks(List<ScrapedRobotGameScoreDto> dtos) {
        // 1. Map the raw data into the wrappers and figure out the 1st, 2nd, and 3rd best runs
        // We want highest scores first!
        List<ProcessedRobotGameScoreDto> processedList = dtos.stream().map(dto -> {
            // Put the runs in an array and sort them.
            // Arrays.sort() sorts ascending, so runs[2] is the highest score.
            int[] runs = {dto.getRun1(), dto.getRun2(), dto.getRun3()};
            Arrays.sort(runs);

            return ProcessedRobotGameScoreDto.builder()
                .fllId(dto.getFllId())
                .robotGameScoreDto(dto) // Your original raw data
                .bestRun(runs[2])       // Highest
                .secondBestRun(runs[1]) // Middle
                .thirdBestRun(runs[0])  // Lowest
                .build();
        }).sorted(Comparator
            .comparingInt(ProcessedRobotGameScoreDto::getBestRun)
            .thenComparingInt(ProcessedRobotGameScoreDto::getSecondBestRun)
            .thenComparingInt(ProcessedRobotGameScoreDto::getThirdBestRun)
            .reversed()).collect(Collectors.toList());

        // 2. Sort the list descending based on FLL tiebreaker rules

        // 3. Apply Standard Competition Ranking (1, 2, 2, 4...)
        for (int i = 0; i < processedList.size(); i++) {
            ProcessedRobotGameScoreDto current = processedList.get(i);

            if (i == 0) {
                // The first team is always rank 1
                current.setPrelimRank(1);
            } else {
                ProcessedRobotGameScoreDto previous = processedList.get(i - 1);

                // Check for a perfect tie across all three runs
                boolean isTied = current.getBestRun() == previous.getBestRun() &&
                    current.getSecondBestRun() == previous.getSecondBestRun() &&
                    current.getThirdBestRun() == previous.getThirdBestRun();

                if (isTied) {
                    // Give them the exact same rank as the team above them
                    current.setPrelimRank(previous.getPrelimRank());
                } else {
                    // Not tied? Their rank is simply their index in the sorted list + 1.
                    // (e.g., if they are at index 3, they are rank 4)
                    current.setPrelimRank(i + 1);
                }
            }
        }

        return processedList;
    }

    private void syncNominations(Competition comp, ScrapedAwardsAndRanksDto data, Map<String, SeasonTeam> teamMap) {
        // Delete Old
        placeRepository.deleteByCompetition(comp);
        placeRepository.flush();
        nominationRepository.deleteByCompetition(comp);
        nominationRepository.flush();

        // Save Places
        List<Place> places = new ArrayList<>();
        for (ScrapedPlaceDto dto : data.getPlaces()) {
            SeasonTeam team = resolveSeasonTeamByName(comp, teamMap, dto.getTeamName());
            if (team != null) {
                places.add(Place.builder().competition(comp).seasonTeam(team)
                    .place(dto.getPlace()).advancing(dto.isAdvancing()).build());
            }
        }
        placeRepository.saveAll(places);

        // Save Nominations
        List<Nomination> nominations = new ArrayList<>();
        for (ScrapedNominationDto dto : data.getNominations()) {
            SeasonTeam team = resolveSeasonTeamByName(comp, teamMap, dto.getTeamName());
            if (team != null) {
                nominations.add(Nomination.builder().competition(comp).seasonTeam(team)
                    .category(dto.getCategory()).isAwardWinner(dto.isWinner()).build());
            }
        }
        nominationRepository.saveAll(nominations);
    }

    private void updateTeamFields(SeasonTeam team, ScrapedTeamDto dto) {
        team.setName(dto.getName());
        team.setInstitution(dto.getInstitution());
        team.setCity(dto.getCity());
        if (dto.getCountry() != null) {
            team.setCountry(dto.getCountry());
        }
        if (dto.getLinks() != null) {
            team.setLinks(mapLinks(dto.getLinks()));
        }

        if (dto.getName().isBlank()) {
            Optional<NameChange> nameChange = nameChanges.stream()
                .filter(nc -> nc.seasonId().equals(team.getSeason().getId()) && nc.fllId().equals(team.getFllId()))
                .findFirst();
            if (nameChange.isPresent()) {
                team.setName(nameChange.get().oldName());
                log.info("⚠️ Team with FLL ID '{}' has blank name. Using old name from name change record: '{}'", team.getFllId(), team.getName());
            } else {
                log.warn("Team with FLL ID '{}' has blank name and no name change record. This may cause issues with score/award matching.", team.getFllId());
            }
        }
    }

    private List<Link> mapLinks(List<ScrapedLinkDto> dtos) {
        return dtos.stream()
            .map(d -> Link.builder().label(d.getLabel()).url(d.getUrl()).build())
            .collect(Collectors.toList());
    }
}