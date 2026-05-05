package org.cthub.backend.migration;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Nomination;
import org.cthub.backend.model.Place;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.NominationRepository;
import org.cthub.backend.repository.PlaceRepository;
import org.cthub.backend.repository.RobotGameResultRepository;
import org.cthub.backend.repository.SeasonTeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddMecklenburgVorpommern2026 implements CommandLineRunner {

    record TeamData(
        String fllId,
        int best_pr,
        int prelim_rank,
        int pr1,
        int pr2,
        int pr3,
        int playoff_rank,
        Integer qf,
        Integer sf,
        Integer f1,
        Integer f2
    ) {}

    record NominationData(
        Nomination.AwardCategory category,
        boolean isAwardWinner,
        String fllId
    ) {}

    record PlaceData(
        int place,
        boolean advancing,
        String fllId
    ) {}

    private static final List<TeamData> TEAMS_TO_ADD = List.of(
        new TeamData("1090", 270, 2, 190, 270, 190, 2, null, null, 280, 170),
        new TeamData("1605", 410, 1, 335, 350, 410, 1, null, null, 255, 320)
    );

    private static final List<NominationData> NOMINATIONS = List.of(
        new NominationData(Nomination.AwardCategory.RESEARCH, true, "1090"),
        new NominationData(Nomination.AwardCategory.RESEARCH, false, "1605"),
        new NominationData(Nomination.AwardCategory.ROBOT_DESIGN, false, "1090"),
        new NominationData(Nomination.AwardCategory.ROBOT_DESIGN, true, "1605"),
        new NominationData(Nomination.AwardCategory.CORE_VALUES, false, "1090"),
        new NominationData(Nomination.AwardCategory.CORE_VALUES, true, "1605"),

        new NominationData(Nomination.AwardCategory.ROBOT_GAME, true, "1605"),
        new NominationData(Nomination.AwardCategory.COACHING, true, "1090"),
        new NominationData(Nomination.AwardCategory.CHAMPION, true, "1090")
    );

    private static final List<PlaceData> PLACES = List.of(
        new PlaceData(1, true, "1090"),
        new PlaceData(2, true, "1605")
    );

    private final RobotGameResultRepository robotGameResultRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final CompetitionRepository competitionRepository;
    private final PlaceRepository placeRepository;
    private final NominationRepository nominationRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Competition comp = competitionRepository.findByUrlPartAndSeasonId("mecklenburg-vorpommern", "2025-26").orElseThrow();

        Map<String, SeasonTeam> teamsMap = seasonTeamRepository.findByRegisteredCompetitionsContains(comp)
            .stream()
            .collect(Collectors.toMap(SeasonTeam::getFllId, Function.identity()));

        List<RobotGameResult> results = robotGameResultRepository.findAllByCompetitionId(comp.getId());

        if(!results.isEmpty()) {
            log.info("Robot game results for competition {} already exist. Skipping.", comp.getName());
            return;
        }

        List<RobotGameResult> rg_list = new ArrayList<>();
        for (TeamData team : TEAMS_TO_ADD) {
            SeasonTeam seasonTeam = teamsMap.get(team.fllId);

            rg_list.add(RobotGameResult.builder().competition(comp).seasonTeam(seasonTeam)
                .pr1(team.pr1).pr2(team.pr2).pr3(team.pr3).bestPr(team.best_pr)
                .qf(team.qf).sf(team.sf).f1(team.f1).f2(team.f2)
                .rank(team.playoff_rank).prelimRank(team.prelim_rank)
                .build());
        }
        robotGameResultRepository.saveAll(rg_list);

        List<Place> places = new ArrayList<>();
        for (PlaceData placeData : PLACES) {
            SeasonTeam seasonTeam = teamsMap.get(placeData.fllId);
            places.add(Place.builder().competition(comp).seasonTeam(seasonTeam).place(placeData.place).advancing(placeData.advancing).build());
        }
        placeRepository.saveAll(places);

        List<Nomination> nominations = new ArrayList<>();
        for (NominationData nominationData : NOMINATIONS) {
            SeasonTeam seasonTeam = teamsMap.get(nominationData.fllId);
            nominations.add(Nomination.builder().competition(comp).seasonTeam(seasonTeam).category(nominationData.category).isAwardWinner(nominationData.isAwardWinner).build());
        }
        nominationRepository.saveAll(nominations);

        comp.setResultsAvailable(true);
        competitionRepository.save(comp);
    }
}


