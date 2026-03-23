package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonTeamRepository extends JpaRepository<SeasonTeam, Long> {
    List<SeasonTeam> findByRegisteredCompetitionsContains(Competition competition);

        @Query("SELECT DISTINCT st FROM SeasonTeam st " +
        "LEFT JOIN FETCH st.seasonTeamProfile stp " + // Eager fetch the profile
        "LEFT JOIN FETCH stp.teamProfile " +
        "LEFT JOIN FETCH st.links " +       // Eager fetch the links
        "JOIN st.registeredCompetitions c " + // Normal join just for the WHERE clause
        "WHERE c.id = :competitionId " +
        "ORDER BY st.fllId ASC")
    List<SeasonTeam> findRegisteredTeamsByCompetitionId(@Param("competitionId") Long competitionId);

    List<SeasonTeam> findBySeasonIdAndFllIdIn(String seasonId, List<String> fllIds);

    @Query("SELECT DISTINCT st FROM SeasonTeam st " +
        "JOIN FETCH st.season s " +                // Fetch the season (needed for the WHERE clause and DTO)
        "LEFT JOIN FETCH st.seasonTeamProfile stp " + // Eager fetch the profile
        "LEFT JOIN FETCH stp.teamProfile " +
        "LEFT JOIN FETCH st.registeredCompetitions " + // Eager fetch the competitions!
        "WHERE s.id = :seasonId AND st.fllId = :fllId")
    Optional<SeasonTeam> findBySeasonIdAndFllIdWithDetails(
        @Param("seasonId") String seasonId,
        @Param("fllId") String fllId
    );

    @Query("SELECT st FROM SeasonTeam st " +
        "JOIN FETCH st.season s " +                // Fetch the season (needed for the WHERE clause and DTO)
        "JOIN FETCH st.seasonTeamProfile stp " + // Eager fetch the profile
        "JOIN FETCH stp.teamProfile tp " +        // Eager fetch the team profile
        "LEFT JOIN FETCH st.registeredCompetitions " + // Eager fetch the competitions!
        "WHERE tp.id = :profileId")
    List<SeasonTeam> findByTeamProfileId(@Param("profileId") Long profileId);


}