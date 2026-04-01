package org.cthub.backend.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "season_team_id", "competition_id" }) })
public class RobotGameResult {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rg_seq_gen")
    @SequenceGenerator(name = "rg_seq_gen", sequenceName = "rg_seq")
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private SeasonTeam seasonTeam; // The specific entry for this year

    @NotNull
    @ManyToOne(optional = false)
    private Competition competition;

    // Preliminary Rounds
    @NotNull
    @Column(nullable = false)
    private int pr1;
    @NotNull
    @Column(nullable = false)
    private int pr2;
    @NotNull
    @Column(nullable = false)
    private int pr3;

    @NotNull
    @Column(nullable = false)
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

    @NotNull
    @Column(nullable = false)
    private int rank; // 1, 2, 3... null if not ranked

    @NotNull
    @Column(nullable = false)
    private int prelimRank;
}