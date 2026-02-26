package org.cthub.backend.repository;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    List<Competition> findAllBySeason(Season season);

    @Query("SELECT c FROM Competition c " +
           "WHERE c.resultsAvailable = false " +
           "AND c.date <= :currentDate")
    List<Competition> findPendingResults(LocalDate currentDate);

    @Query("SELECT c FROM Competition c " +
        "JOIN FETCH c.season s " +
        "LEFT JOIN FETCH c.links " +
        "WHERE c.urlPart = :urlPart AND s.id = :seasonId")
    Optional<Competition> findByUrlPartAndSeasonId(
        @Param("urlPart") String urlPart,
        @Param("seasonId") String seasonId);


    int countBySeasonAndActiveTrue(Season season);
}