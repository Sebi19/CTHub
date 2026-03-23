package org.cthub.backend.repository;

import org.cthub.backend.model.SeasonTeamProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonTeamProfileRepository extends JpaRepository<SeasonTeamProfile, Long> {

}