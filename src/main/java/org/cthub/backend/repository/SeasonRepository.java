package org.cthub.backend.repository;

import org.cthub.backend.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, String> {
    Optional<Season> findByActiveTrue();
}