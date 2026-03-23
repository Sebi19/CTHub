package org.cthub.backend.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.repository.RobotGameResultRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackfillPrelimRanksTask implements CommandLineRunner {

    private final RobotGameResultRepository repository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // Check if migration is needed (if there are records where prelimRank is null)
        long nullCount = repository.findAll().stream()
            .filter(r -> r.getPrelimRank() == null)
            .count();

        if (nullCount == 0) {
            log.info("⏭️ No empty prelimRanks found. Migration skipped.");
            return;
        }

        log.info("⚙️ Starting PrelimRank Backfill Migration for {} records...", nullCount);

        // 1. Fetch ALL results
        List<RobotGameResult> allResults = repository.findAll();

        // 2. Group them by Competition ID
        Map<Long, List<RobotGameResult>> resultsByCompetition = allResults.stream()
            .collect(Collectors.groupingBy(r -> r.getCompetition().getId()));

        // 3. Process each competition independently
        for (Map.Entry<Long, List<RobotGameResult>> entry : resultsByCompetition.entrySet()) {
            List<RobotGameResult> compResults = getSortedRobotGameResults(entry);

            // 5. Apply the Standard Competition Ranking (1, 2, 2, 4...)
            for (int i = 0; i < compResults.size(); i++) {
                RobotGameResult current = compResults.get(i);

                if (i == 0) {
                    current.setPrelimRank(1);
                } else {
                    RobotGameResult previous = compResults.get(i - 1);

                    int[] currRuns = getSortedRuns(current);
                    int[] prevRuns = getSortedRuns(previous);

                    boolean isTied = currRuns[2] == prevRuns[2] &&
                        currRuns[1] == prevRuns[1] &&
                        currRuns[0] == prevRuns[0];

                    if (isTied) {
                        current.setPrelimRank(previous.getPrelimRank());
                    } else {
                        current.setPrelimRank(i + 1);
                    }
                }
            }
        }

        // 6. Save all updated records back to the database in a batch
        repository.saveAll(allResults);

        log.info("✅ PrelimRank Backfill Migration Complete! You can delete BackfillPrelimRanksTask.java now.");
    }

    private List<RobotGameResult> getSortedRobotGameResults(Map.Entry<Long, List<RobotGameResult>> entry) {
        List<RobotGameResult> compResults = entry.getValue();

        // 4. Sort using the exact tiebreaker logic we built earlier
        compResults.sort((r1, r2) -> {
            int[] runs1 = getSortedRuns(r1);
            int[] runs2 = getSortedRuns(r2);

            // Compare Best Run (Index 2)
            if (runs1[2] != runs2[2]) return Integer.compare(runs2[2], runs1[2]);
            // Compare Second Best (Index 1)
            if (runs1[1] != runs2[1]) return Integer.compare(runs2[1], runs1[1]);
            // Compare Third Best (Index 0)
            return Integer.compare(runs2[0], runs1[0]);
        });
        return compResults;
    }

    // Helper method to easily extract the 1st, 2nd, and 3rd best runs
    private int[] getSortedRuns(RobotGameResult r) {
        int[] runs = {r.getPr1(), r.getPr2(), r.getPr3()};
        Arrays.sort(runs);
        return runs;
    }
}