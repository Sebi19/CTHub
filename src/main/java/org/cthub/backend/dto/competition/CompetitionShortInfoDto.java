package org.cthub.backend.dto.competition;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.season.SeasonDto;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionShortInfoDto {
    @NotNull
    private Long id;
    @NotNull
    private SeasonDto season;
    @NotNull
    private String name;
    @NotNull
    private String urlPart;

    private String slug; // optional, only for competitions that have a slug in the new API

    @NotNull
    private CompetitionType type;
    @NotNull
    private boolean active;
    @NotNull
    private boolean resultsAvailable;

    private String country;
    private LocalDate date;
    private LocalDate endDate; // null means same as date

    private int registeredTeamCount;
    private int maxTeamCount;

    @NotNull
    boolean newApiCompetition; // every competition since 2026-27
}
