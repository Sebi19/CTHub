package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    // To check if we already scraped this event (using the URL part)
    Optional<Competition> findByUrlPart(String urlPart);
}