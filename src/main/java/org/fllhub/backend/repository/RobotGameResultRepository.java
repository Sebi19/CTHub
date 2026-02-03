package org.fllhub.backend.repository;

import org.fllhub.backend.model.Competition;
import org.fllhub.backend.model.RobotGameResult;
import org.fllhub.backend.model.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RobotGameResultRepository extends JpaRepository<RobotGameResult, Long> {
    Optional<RobotGameResult> findByCompetitionAndSeasonTeam(Competition competition, SeasonTeam seasonTeam);

    // 1. Filter by Season directly in the WHERE clause
    // 2. LEFT JOIN Place to find the matching entry for this Team + Competition
    // 3. COALESCE handles nulls (if no Place entry exists, they didn't advance -> false)
    @Query("SELECT r, COALESCE(p.advancing, false) " +
        "FROM RobotGameResult r " +
        "JOIN FETCH r.seasonTeam " +
        "JOIN FETCH r.competition " +
        "JOIN FETCH r.competition.season " +
        "LEFT JOIN Place p ON p.seasonTeam = r.seasonTeam AND p.competition = r.competition " +
        "WHERE r.competition.season.id = :seasonId")
    List<Object[]> findAllBySeasonIdWithQualification(@Param("seasonId") String seasonId);
}