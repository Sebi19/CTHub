package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    void deleteByCompetition(Competition competition);

    List<Place> findAllByCompetitionId(Long compId);
    @Query("SELECT p FROM Place p JOIN FETCH p.competition WHERE p.seasonTeam.id = :teamId")
    List<Place> findBySeasonTeamId(@Param("teamId") Long teamId);

    @Query("SELECT p FROM Place p JOIN FETCH p.competition WHERE p.seasonTeam.id IN :teamIds")
    List<Place> findBySeasonTeamIdIn(@Param("teamIds") List<Long> teamIds);
}