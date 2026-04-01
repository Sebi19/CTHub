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


    @Query("""
        SELECT st,
        GREATEST(
            CASE
                WHEN LOWER(st.name) = LOWER(:query) THEN 1.0
                WHEN LOWER(st.name) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('% ', :query, '%')) THEN 0.8
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('%-', :query, '%')) THEN 0.8
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0.7
                ELSE 0.0
            END,
            FUNCTION('similarity', st.name, :query),
            
            CASE
                WHEN LOWER(st.fllId) = LOWER(:query) THEN 1.0
                WHEN LOWER(st.fllId) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                ELSE 0.0
            END,
            
            (CAST(FUNCTION('similarity', COALESCE(st.city, ''), :query) AS Double) * 0.8),
            (CAST(FUNCTION('similarity', COALESCE(st.institution, ''), :query) AS Double) * 0.8)
        ) as match_score
        
        FROM SeasonTeam st
        JOIN FETCH st.season s
        LEFT JOIN FETCH st.seasonTeamProfile stp
        LEFT JOIN FETCH stp.teamProfile
        
        WHERE (:seasonId IS NULL OR s.id = :seasonId)
        
        AND GREATEST(
            CASE
                WHEN LOWER(st.name) = LOWER(:query) THEN 1.0
                WHEN LOWER(st.name) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('% ', :query, '%')) THEN 0.8
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('%-', :query, '%')) THEN 0.8
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0.7
                ELSE 0.0
            END,
            FUNCTION('similarity', st.name, :query),
            CASE
                WHEN LOWER(st.fllId) = LOWER(:query) THEN 1.0
                WHEN LOWER(st.fllId) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                ELSE 0.0
            END,
            (CAST(FUNCTION('similarity', COALESCE(st.city, ''), :query) AS Double) * 0.8),
            (CAST(FUNCTION('similarity', COALESCE(st.institution, ''), :query) AS Double) * 0.8)
        ) >= :threshold
        
        ORDER BY GREATEST(
            CASE
                WHEN LOWER(st.name) = LOWER(:query) THEN 1.0
                WHEN LOWER(st.name) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('% ', :query, '%')) THEN 0.8
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('%-', :query, '%')) THEN 0.8
                WHEN LOWER(st.name) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0.7
                ELSE 0.0
            END,
            FUNCTION('similarity', st.name, :query),
            CASE
                WHEN LOWER(st.fllId) = LOWER(:query) THEN 1.0
                WHEN LOWER(st.fllId) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                ELSE 0.0
            END,
            (CAST(FUNCTION('similarity', COALESCE(st.city, ''), :query) AS Double) * 0.8),
            (CAST(FUNCTION('similarity', COALESCE(st.institution, ''), :query) AS Double) * 0.8)
        ) DESC
        """)
    List<Object[]> searchFuzzySeasonTeams(
        @Param("query") String query,
        @Param("seasonId") String seasonId,
        @Param("threshold") double threshold
    );


}