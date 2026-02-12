package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonTeamRepository extends JpaRepository<SeasonTeam, Long> {
    List<SeasonTeam> findByRegisteredCompetitionsContains(Competition competition);

    List<SeasonTeam> findBySeasonIdAndFllIdIn(String seasonId, List<String> fllIds);
}