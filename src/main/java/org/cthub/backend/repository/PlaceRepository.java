package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    void deleteByCompetition(Competition competition);
}