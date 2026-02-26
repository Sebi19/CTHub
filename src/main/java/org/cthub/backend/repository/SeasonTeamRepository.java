package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonTeamRepository extends JpaRepository<SeasonTeam, Long> {
    List<SeasonTeam> findByRegisteredCompetitionsContains(Competition competition);

        @Query("SELECT DISTINCT st FROM SeasonTeam st " +
        "LEFT JOIN FETCH st.teamProfile " + // Eager fetch the profile
        "LEFT JOIN FETCH st.links " +       // Eager fetch the links
        "JOIN st.registeredCompetitions c " + // Normal join just for the WHERE clause
        "WHERE c.id = :competitionId")
    List<SeasonTeam> findRegisteredTeamsByCompetitionId(@Param("competitionId") Long competitionId);

    List<SeasonTeam> findBySeasonIdAndFllIdIn(String seasonId, List<String> fllIds);
}