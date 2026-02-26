package org.cthub.backend.dto.robotgame;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OverallRobotGameEntryDto {
    // Basic Info
    private int rank;           // Overall Rank (#Best)
    private String teamName;
    private String teamId;      // e.g. "1395"
    private String competition;    // "Ortenau", "Windisch"
    private String competitionUrlPart; // "ortenau" (for links)
    private String country;     // "DE", "CH"
    private boolean qualified;  // For the Green/Red styling

    // Calculated Stats
    private int bestScore;
    private double medianScore; // "Med"
    private double averageScore; // "Midd" (assuming Middle/Mean)
    private int worstScore;

    // Raw Round Scores (for the detailed columns)
    private Integer preliminaryRound1;       // R1
    private Integer preliminaryRound2;       // R2
    private Integer preliminaryRound3;       // R3
    private Integer bestPreliminaryScore;    // Best R1-R3
    private Integer roundOf16;   // R16
    private Integer quarterFinal; // VF
    private Integer semiFinal;    // HF
    private Integer final1;       // F I
    private Integer final2;       // F II
}
