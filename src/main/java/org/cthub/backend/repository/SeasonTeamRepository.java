package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SeasonTeamRepository extends JpaRepository<SeasonTeam, Long> {
    // Vital: Check if "Team 1046" already exists in "Season 2024"
    Optional<SeasonTeam> findBySeasonIdAndFllId(String seasonId, String fllId);

    Optional<SeasonTeam> findByRegisteredCompetitionsContainsAndNameIgnoreCase(Competition competition, String name);
}