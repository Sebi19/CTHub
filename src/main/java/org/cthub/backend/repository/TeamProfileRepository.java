package org.cthub.backend.repository;

import org.cthub.backend.model.TeamProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamProfileRepository extends JpaRepository<TeamProfile, Long> {
}