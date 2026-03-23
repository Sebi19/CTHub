package org.cthub.backend.dto.competition;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.common.LinkDto;
import org.cthub.backend.dto.season.SeasonDto;
import org.cthub.backend.dto.team.SeasonTeamDto;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionDetailDto {
    @NotNull
    private Long id;
    @NotNull
    private SeasonDto season;
    @NotNull
    private String name;
    @NotNull
    private String urlPart;

    @NotNull
    private CompetitionType type;
    @NotNull
    boolean active;

    private CompetitionShortInfoDto nextCompetition;
    private List<CompetitionShortInfoDto> previousCompetitions;

    private String country;
    private LocalDate date;
    private LocalDate endDate; // null means same as date
    private CompetitionContactInfoDto contactInfo;
    private String location;

    @NotNull
    private List<LinkDto> links;

    @NotNull
    private List<SeasonTeamDto> registeredTeams;
    private CompetitionResultsDto results; // null if results not published yet


    private int registeredTeamCount;
    private int maxTeamCount;
}