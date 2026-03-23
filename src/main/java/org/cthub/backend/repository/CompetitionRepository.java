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
            "JOIN FETCH c.season s " +
           "WHERE c.resultsAvailable = false " +
           "AND c.date <= :currentDate " +
           "AND s.id = :seasonId")
    List<Competition> findPendingResults(
        @Param("currentDate") LocalDate currentDate,
        @Param("seasonId") String seasonId);

    @Query("SELECT c FROM Competition c " +
        "JOIN FETCH c.season s " +
        "LEFT JOIN FETCH c.links " +
        "WHERE c.urlPart = :urlPart AND s.id = :seasonId")
    Optional<Competition> findByUrlPartAndSeasonId(
        @Param("urlPart") String urlPart,
        @Param("seasonId") String seasonId);

    @Query("SELECT c FROM Competition c " +
        "JOIN FETCH c.season s " +
        "WHERE c.active = true " +
        "AND c.qualificationUrlPart = :qualificationUrlPart AND s.id = :seasonId")
    List<Competition> findAllBySeasonIdAndQualificationUrlPart(
        @Param("seasonId") String seasonId,
        @Param("qualificationUrlPart") String qualificationUrlPart);

    @Query("SELECT c FROM Competition c " +
        "JOIN FETCH c.season s " +
        "WHERE c.active = true " +
        "AND c.urlPart IN :urlParts AND s.id = :seasonId")
    List<Competition> findAllBySeasonIdAndUrlPart(
        @Param("seasonId") String seasonId,
        @Param("urlParts") List<String> urlParts);

    @Query("SELECT c FROM Competition c " +
        "JOIN FETCH c.season s " +
        "WHERE c.active = true " +
        "AND CONCAT(s.id, '/', c.urlPart) IN :seasonAndUrlPairs")
    List<Competition> findAllByPairs(@Param("seasonAndUrlPairs") List<String> seasonAndUrlPairs);


    int countBySeasonAndActiveTrue(Season season);
}