package org.cthub.backend.dto.competition;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionPlaceDto {
    @NotNull
    private Long teamId;
    @NotNull
    private int place;
    @NotNull
    private boolean advancing;
}
