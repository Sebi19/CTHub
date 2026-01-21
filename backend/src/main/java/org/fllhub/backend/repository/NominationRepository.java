package org.fllhub.backend.repository;

import org.fllhub.backend.model.Competition;
import org.fllhub.backend.model.Nomination;
import org.fllhub.backend.model.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NominationRepository extends JpaRepository<Nomination, Long> {
    Optional<Nomination> findByCompetitionAndSeasonTeamAndCategory(Competition competition, SeasonTeam seasonTeam, Nomination.AwardCategory category);

    void deleteByCompetition(Competition competition);
}