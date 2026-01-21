package org.fllhub.backend.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "season_team_id", "competition_id" }) })
public class RobotGameResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private SeasonTeam seasonTeam; // The specific entry for this year

    @ManyToOne(optional = false)
    private Competition competition;

    // Preliminary Rounds
    private int pr1;
    private int pr2;
    private int pr3;

    // We can persist this to make sorting easier
    private int bestPr;

    // Finals (Nullable because most won't reach them)
    @Nullable
    private Integer r16;

    @Nullable
    private Integer qf;

    @Nullable
    private Integer sf;

    @Nullable
    private Integer f1;

    @Nullable
    private Integer f2;

    private Integer rank; // 1, 2, 3... null if not ranked
}