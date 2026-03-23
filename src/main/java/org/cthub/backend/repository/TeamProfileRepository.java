package org.cthub.backend.repository;

import org.cthub.backend.model.TeamProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamProfileRepository extends JpaRepository<TeamProfile, Long> {
    Optional<TeamProfile> findByCustomUrl(String customUrl);
}