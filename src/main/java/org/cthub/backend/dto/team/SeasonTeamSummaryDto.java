package org.cthub.backend.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.season.SeasonDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeasonTeamSummaryDto {
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
    private String country;

    private SeasonTeamProfileDto seasonTeamProfile; // null if no profile exists
}
