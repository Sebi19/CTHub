package org.cthub.backend.dto.competition;

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
    private List<CompetitionPlaceDto> places;
    private List<CompetitionNominationDto> nominations;
    private List<CompetitionRobotGameEntryDto> robotGameEntries;
}
