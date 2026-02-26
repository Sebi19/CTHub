package org.cthub.backend.dto.competition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionNominationDto {
    private Long teamId;
    private CompetitionAwardCategoryDto category;
    private boolean isWinner;
}
