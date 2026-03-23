package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Nomination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NominationRepository extends JpaRepository<Nomination, Long> {
    void deleteByCompetition(Competition competition);

    List<Nomination> findAllByCompetitionId(Long compId);

    @Query("SELECT n FROM Nomination n JOIN FETCH n.competition WHERE n.seasonTeam.id = :teamId")
    List<Nomination> findBySeasonTeamIdWithCompetition(@Param("teamId") Long teamId);

    @Query("SELECT n FROM Nomination n JOIN FETCH n.competition WHERE n.seasonTeam.id IN :teamIds")
    List<Nomination> findBySeasonTeamIdInWithCompetition(@Param("teamIds") List<Long> teamIds);
}