package org.cthub.backend.dto.competition;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionResultsDto {
    @NotNull
    private List<CompetitionPlaceDto> places;
    @NotNull
    private List<CompetitionNominationDto> nominations;
    @NotNull
    private List<CompetitionRobotGameEntryDto> robotGameEntries;
}
