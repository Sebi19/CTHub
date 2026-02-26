package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Nomination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NominationRepository extends JpaRepository<Nomination, Long> {
    void deleteByCompetition(Competition competition);

    List<Nomination> findAllByCompetitionId(Long compId);
}