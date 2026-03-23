package org.cthub.backend.dto.robotgame;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.competition.CompetitionShortInfoDto;
import org.cthub.backend.dto.team.SeasonTeamDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OverallRobotGameEntryDto {
    // Basic Info
    @NotNull
    private int rank;           // Overall Rank (#Best)
    @NotNull
    private SeasonTeamDto team;
    @NotNull
    private CompetitionShortInfoDto competition;
    private String country;     // "DE", "CH"
    @NotNull
    private boolean qualified;  // For the Green/Red styling

    // Calculated Stats
    @NotNull
    private int bestScore;
    @NotNull
    private double medianScore; // "Med"
    @NotNull
    private double averageScore; // "Midd" (assuming Middle/Mean)
    @NotNull
    private int worstScore;

    // Raw Round Scores (for the detailed columns)
    @NotNull
    private Integer preliminaryRound1;       // R1
    @NotNull
    private Integer preliminaryRound2;       // R2
    @NotNull
    private Integer preliminaryRound3;       // R3
    @NotNull
    private Integer bestPreliminaryScore;    // Best R1-R3
    private Integer roundOf16;   // R16
    private Integer quarterFinal; // VF
    private Integer semiFinal;    // HF
    private Integer final1;       // F I
    private Integer final2;       // F II
}
