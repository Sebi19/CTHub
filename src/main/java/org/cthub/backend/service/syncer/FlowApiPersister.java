package org.cthub.backend.service.syncer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Season;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.SeasonTeamRepository;
import org.cthub.backend.service.syncer.dto.DrahtVenueDto;
import org.cthub.backend.service.syncer.dto.FlowEventDto;
import org.cthub.backend.service.syncer.dto.FlowPublicInfoDto;
import org.cthub.backend.service.syncer.dto.FlowTeamDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowApiPersister {

    private final CompetitionRepository competitionRepository;
    private final SeasonTeamRepository seasonTeamRepository;

    /**
     * @return true if the event was successfully synced and is active, false if skipped/deactivated.
     */
    @Transactional
    public boolean upsertEventAndTeams(Season activeSeason, FlowEventDto eventDto, FlowPublicInfoDto publicInfo, DrahtVenueDto venueDto) {

        // 1. Find existing competition
        Optional<Competition> existingOpt = competitionRepository.findByFlowId(eventDto.getId());
        if (existingOpt.isEmpty()) {
            existingOpt = competitionRepository.findBySlugAndSeasonId(eventDto.getSlug(), activeSeason.getId());
        }

        // 2. HARD DEPENDENCY CHECK: Missing Venue Data
        if (venueDto == null) {
            if (existingOpt.isPresent()) {
                Competition comp = existingOpt.get();
                if (comp.isActive()) {
                    comp.setActive(false);
                    competitionRepository.save(comp);
                    log.info("📉 Deactivated {} because Draht venue data went missing.", comp.getName());
                }
            } else {
                log.warn("⏭️ Skipped new event {} because Draht venue data is missing.", eventDto.getName());
            }
            return false;
        }

        // 3. UPSERT ACTIVE COMPETITION
        Competition comp = existingOpt.orElseGet(() -> Competition.builder().season(activeSeason).build());

        comp.setFlowId(eventDto.getId());
        comp.setChallengeId(eventDto.getChallengeId());
        comp.setSlug(eventDto.getSlug());
        comp.setUrlPart(eventDto.getSlug().replace("-challenge", "")); // Fallback for old routing logic until fully migrated
        comp.setName(eventDto.getName());
        comp.setActive(true);

        // Map Level to CompetitionType
        if (eventDto.getLevelRel() != null && eventDto.getLevelRel().getId() != null) {
            int level = eventDto.getLevelRel().getId();
            if (level == 1) {
                comp.setType(Competition.CompetitionType.REGIONAL);
            } else if (level == 2) {
                comp.setType(Competition.CompetitionType.QUALIFICATION);
            } else {
                comp.setType(Competition.CompetitionType.FINAL);
            }
        }

        comp.setDate(parseEventDate(venueDto.getDate()));
        comp.setEndDate(parseEventDate(venueDto.getEndDate()));

        if (venueDto.getCountry() != null) {
            comp.setCountry(venueDto.getCountry().toUpperCase());
        }

        comp.setLatitude(venueDto.getLat());
        comp.setLongitude(venueDto.getLon());

        comp.setMaxTeamCount(venueDto.getCapacity());
        comp.setRegisteredTeamCount(venueDto.getRegistered());

        // Map Details from Public Info
        if (publicInfo != null) {
            comp.setLocation(publicInfo.getAddress());

            if (publicInfo.getContact() != null && !publicInfo.getContact().isEmpty()) {
                Competition.ContactInfo contact = new Competition.ContactInfo();
                contact.setContactName(publicInfo.getContact().getFirst().getContact());
                contact.setContactEmail(publicInfo.getContact().getFirst().getContactEmail());
                comp.setContact(contact);
            }
        }

        competitionRepository.save(comp);

        // 2. UPSERT TEAMS
        if (publicInfo != null && publicInfo.getTeams() != null && publicInfo.getTeams().getChallenge() != null) {
            syncTeams(comp, activeSeason, publicInfo.getTeams().getChallenge().getList());
        }

        return true;
    }

    /**
     * Soft deletes any active competitions in the DB that were not part of the current API sync.
     */
    @Transactional
    public void deactivateMissingEvents(Season season, Set<String> successfullySyncedSlugs) {
        List<Competition> existingComps = competitionRepository.findAllBySeason(season);
        boolean changesMade = false;

        for (Competition comp : existingComps) {
            // If it's active in the DB but wasn't in the API response
            if (comp.isActive() && !successfullySyncedSlugs.contains(comp.getSlug())) {
                log.info("📉 Competition disappeared from API list: {} (Setting inactive)", comp.getName());
                comp.setActive(false);
                changesMade = true;
            }
        }

        if (changesMade) {
            competitionRepository.saveAll(existingComps);
        }
    }

    private void syncTeams(Competition comp, Season season, List<FlowTeamDto> apiTeams) {
        if (apiTeams == null) return;

        // Fetch existing teams for this competition
        Map<String, SeasonTeam> existingTeams = seasonTeamRepository.findByRegisteredCompetitionsContains(comp)
            .stream()
            .collect(Collectors.toMap(SeasonTeam::getFllId, Function.identity()));

        Set<String> processedFllIds = new HashSet<>();

        for (FlowTeamDto apiTeam : apiTeams) {
            processedFllIds.add(apiTeam.getFllId());

            // Check if team already linked, otherwise check globally in this season, otherwise create new
            SeasonTeam team = existingTeams.get(apiTeam.getFllId());
            if (team == null) {
                team = seasonTeamRepository.findBySeasonIdAndFllIdWithDetails(season.getId(), apiTeam.getFllId())
                    .orElseGet(() -> SeasonTeam.builder()
                        .season(season)
                        .fllId(apiTeam.getFllId())
                        .registeredCompetitions(new HashSet<>())
                        .build());
            }

            team.setName(apiTeam.getName());
            team.setInstitution(apiTeam.getOrganization());
            team.setCity(apiTeam.getLocation());
            team.setActive(true);

            team.getRegisteredCompetitions().add(comp);
            seasonTeamRepository.save(team);
        }

        // Remove dropped out teams
        for (SeasonTeam existing : existingTeams.values()) {
            if (!processedFllIds.contains(existing.getFllId())) {
                existing.getRegisteredCompetitions().remove(comp);

                if (existing.getRegisteredCompetitions().isEmpty()) {
                    existing.setActive(false);
                }
                seasonTeamRepository.save(existing);
            }
        }
    }

    private LocalDate parseEventDate(String dateStr) {
        // Catch nulls, empty strings, and the Unix Epoch fallback
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equals("1970-01-01")) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Could not parse date: {}", dateStr);
            return null;
        }
    }
}