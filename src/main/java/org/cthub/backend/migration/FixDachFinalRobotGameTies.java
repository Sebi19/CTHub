package org.cthub.backend.migration;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Nomination;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.NominationRepository;
import org.cthub.backend.repository.RobotGameResultRepository;
import org.cthub.backend.repository.SeasonTeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixDachFinalRobotGameTies implements CommandLineRunner {

    record RgPlaceOverride(
        String fllId,
        int place
    ) {}

    record AdditionalRobotGameAward(
        String fllId
    ) {}

    private static final List<RgPlaceOverride> RG_PLACE_OVERRIDES = List.of(
        new RgPlaceOverride("1514", 1),
        new RgPlaceOverride("1303", 3)
    );

    private static final List<AdditionalRobotGameAward> ADDITIONAL_ROBOT_GAME_AWARDS = List.of(
        new AdditionalRobotGameAward("1514")
    );

    private final RobotGameResultRepository robotGameResultRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final CompetitionRepository competitionRepository;
    private final NominationRepository nominationRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Competition comp = competitionRepository.findByUrlPartAndSeasonId("finale-2025-26","2025-26").orElseThrow();

        Map<String, SeasonTeam> teamsMap = seasonTeamRepository.findByRegisteredCompetitionsContains(comp)
            .stream()
            .collect(Collectors.toMap(SeasonTeam::getFllId, Function.identity()));

        List<RobotGameResult> results = robotGameResultRepository.findAllByCompetitionId(comp.getId());

        for(RgPlaceOverride override : RG_PLACE_OVERRIDES) {
            RobotGameResult rgr = results.stream()
                .filter(r -> r.getSeasonTeam().getFllId().equals(override.fllId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No RobotGameResult found for team " + override.fllId));

            if(rgr.getRank() == override.place) {
                log.info("Place for team {} is already correct at {}. Skipping.", override.fllId, override.place);
                continue;
            }

            log.info("Overriding place for team {} from {} to {}", override.fllId, rgr.getRank(), override.place);
            rgr.setRank(override.place);
            robotGameResultRepository.save(rgr);
        }

        List<Nomination> existingNominations = nominationRepository.findAllByCompetitionId(comp.getId());

        for(AdditionalRobotGameAward additionalAward : ADDITIONAL_ROBOT_GAME_AWARDS) {
            boolean alreadyHasAward = existingNominations.stream()
                .filter(n -> n.getSeasonTeam().getFllId().equals(additionalAward.fllId))
                .anyMatch(n -> n.getCategory() == Nomination.AwardCategory.ROBOT_GAME && n.isAwardWinner());
            if(alreadyHasAward) {
                log.info("Team {} already has a Robot Game award. Skipping.", additionalAward.fllId);
                continue;
            }
            SeasonTeam seasonTeam = teamsMap.get(additionalAward.fllId);
            Nomination nomination = Nomination.builder()
                .seasonTeam(seasonTeam)
                .competition(comp)
                .category(Nomination.AwardCategory.ROBOT_GAME)
                .isAwardWinner(true)
                .build();
            nominationRepository.save(nomination);
            log.info("Added additional Robot Game award for team {}", additionalAward.fllId);
        }
    }
}


