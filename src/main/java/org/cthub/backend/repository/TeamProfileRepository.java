package org.cthub.backend.repository;

import org.cthub.backend.model.TeamProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamProfileRepository extends JpaRepository<TeamProfile, Long> {
    Optional<TeamProfile> findByCustomUrl(String customUrl);

    @Query("""
        SELECT tp,
        GREATEST(
            CASE
                WHEN LOWER(tp.displayName) = LOWER(:query) THEN 1.0
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('% ', :query, '%')) THEN 0.8
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('%-', :query, '%')) THEN 0.8
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0.7
                ELSE 0.0
            END,
            FUNCTION('similarity', tp.displayName, :query)
        ) as match_score
        
        FROM TeamProfile tp
        
        WHERE (:seasonId IS NULL OR EXISTS (
                    SELECT 1 FROM SeasonTeamProfile stp
                    JOIN stp.seasonTeam st
                    WHERE stp.teamProfile = tp AND st.season.id = :seasonId
                ))
        
        AND GREATEST(
            CASE
                WHEN LOWER(tp.displayName) = LOWER(:query) THEN 1.0
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('% ', :query, '%')) THEN 0.8
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('%-', :query, '%')) THEN 0.8
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0.7
                ELSE 0.0
            END,
            FUNCTION('similarity', tp.displayName, :query)
        ) >= :threshold
        
        ORDER BY GREATEST(
            CASE
                WHEN LOWER(tp.displayName) = LOWER(:query) THEN 1.0
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT(:query, '%')) THEN 0.9
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('% ', :query, '%')) THEN 0.8
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('%-', :query, '%')) THEN 0.8
                WHEN LOWER(tp.displayName) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0.7
                ELSE 0.0
            END,
            FUNCTION('similarity', tp.displayName, :query)
        ) DESC
        """)
    List<Object[]> searchFuzzyTeamProfiles(
        @Param("query") String query,
        @Param("seasonId") String seasonId,
        @Param("threshold") double threshold
    );
}