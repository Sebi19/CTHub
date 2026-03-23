package org.cthub.backend.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.competition.CompetitionNominationDto;
import org.cthub.backend.dto.competition.CompetitionPlaceDto;
import org.cthub.backend.dto.competition.CompetitionRobotGameEntryDto;
import org.cthub.backend.dto.competition.CompetitionShortInfoDto;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamCompetitionRecordDto {
    @NotNull
    private CompetitionShortInfoDto competition;
    private CompetitionPlaceDto place; // null if not placed
    @NotNull
    private List<CompetitionNominationDto> nominations;
    private CompetitionRobotGameEntryDto robotGame; // null if no robot game entry
    private CompetitionShortInfoDto nextCompetition; // null if no next competition is known
}
