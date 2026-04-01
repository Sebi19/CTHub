package org.cthub.backend.dto.search;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.season.SeasonDto;
import org.cthub.backend.dto.team.SeasonTeamProfileDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeasonTeamSearchResultDto {
    @NotNull
    private SeasonDto season;
    @NotNull
    private String fllId;
    @NotNull
    private String name;
    private String institution;
    private String city;
    private String country;
    private SeasonTeamProfileDto seasonTeamProfile;
}
