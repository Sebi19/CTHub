package org.fllhub.backend.repository;

import org.fllhub.backend.model.Competition;
import org.fllhub.backend.model.Place;
import org.fllhub.backend.model.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByCompetitionAndSeasonTeam(Competition competition, SeasonTeam seasonTeam);

    void deleteByCompetition(Competition competition);
}