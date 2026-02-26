package org.cthub.backend.dto.competition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionPlaceDto {
    private Long teamId;
    private int place;
    private boolean advancing;
}
