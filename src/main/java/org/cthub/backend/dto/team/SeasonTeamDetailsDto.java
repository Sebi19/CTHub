package org.cthub.backend.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.common.LinkDto;
import org.cthub.backend.dto.season.SeasonDto;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeasonTeamDetailsDto {
    @NotNull
    private Long id;
    @NotNull
    private SeasonDto season;
    @NotNull
    private boolean active;
    @NotNull
    private String fllId;

    @NotNull
    private String name;
    private String institution;
    private String city;
    private String country;
    @NotNull
    private List<LinkDto> links;

    private SeasonTeamProfileDto seasonTeamProfile; // null if no profile exists

    @NotNull
    private List<TeamCompetitionRecordDto> competitionRecords;
}
