package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    List<Competition> findAllBySeason(Season season);

    @Query("SELECT c FROM Competition c " +
           "WHERE c.resultsAvailable = false " +
           "AND c.date <= :currentDate")
    List<Competition> findPendingResults(LocalDate currentDate);


    int countBySeasonAndActiveTrue(Season season);
}