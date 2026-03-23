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
public class CompetitionRobotGameEntryDto {
    @NotNull
    private Long teamId;
    @NotNull
    private Integer rank;
    @NotNull
    private Integer pr1;
    @NotNull
    private Integer pr2;
    @NotNull
    private Integer pr3;
    @NotNull
    private Integer bestPr;
    private Integer r16;
    private Integer qf;
    private Integer sf;
    private Integer f1;
    private Integer f2;
    // TODO: Set not null once migrated
    private Integer prelimRank;

}
