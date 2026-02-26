package org.cthub.backend.dto.competition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionRobotGameEntryDto {
    private Long teamId;
    private Integer rank;
    private Integer pr1;
    private Integer pr2;
    private Integer pr3;
    private Integer bestPr;
    private Integer r16;
    private Integer qf;
    private Integer sf;
    private Integer f1;
    private Integer f2;

}
