package org.cthub.backend.repository;

import org.cthub.backend.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, String> {
    // To find the season by the integer year (e.g. 2024)
    Optional<Season> findByStartYear(Integer startYear);

    Optional<Season> findByActiveTrue();
}