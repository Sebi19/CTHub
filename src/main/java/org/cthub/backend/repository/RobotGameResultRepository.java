package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.RobotGameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RobotGameResultRepository extends JpaRepository<RobotGameResult, Long> {
    // 1. Filter by Season directly in the WHERE clause
    // 2. LEFT JOIN Place to find the matching entry for this Team + Competition
    // 3. COALESCE handles nulls (if no Place entry exists, they didn't advance -> false)
    @Query("SELECT r, COALESCE(p.advancing, false) " +
        "FROM RobotGameResult r " +
        "JOIN FETCH r.seasonTeam st " +
        "LEFT JOIN FETCH st.seasonTeamProfile stp " +
        "LEFT JOIN FETCH stp.teamProfile tp " +
        "JOIN FETCH r.competition " +
        "JOIN FETCH r.competition.season " +
        "LEFT JOIN Place p ON p.seasonTeam = r.seasonTeam AND p.competition = r.competition " +
        "WHERE r.competition.season.id = :seasonId")
    List<Object[]> findAllBySeasonIdWithQualification(@Param("seasonId") String seasonId);
    void deleteByCompetition(Competition competition);

    List<RobotGameResult> findAllByCompetitionId(Long compId);

    @Query("SELECT rg FROM RobotGameResult rg JOIN FETCH rg.competition WHERE rg.seasonTeam.id = :teamId")
    List<RobotGameResult> findBySeasonTeamIdWithCompetition(@Param("teamId") Long teamId);

    @Query("SELECT rg FROM RobotGameResult rg JOIN FETCH rg.competition WHERE rg.seasonTeam.id IN :teamIds")
    List<RobotGameResult> findBySeasonTeamIdInWithCompetition(@Param("teamIds") List<Long> teamIds);
}