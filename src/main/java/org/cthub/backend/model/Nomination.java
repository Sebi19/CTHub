package org.cthub.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nomination {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nom_seq_gen")
    @SequenceGenerator(name = "nom_seq_gen", sequenceName = "nom_seq")
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private SeasonTeam seasonTeam;

    @NotNull
    @ManyToOne(optional = false)
    private Competition competition;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AwardCategory category;

    @NotNull
    @Column(nullable = false)
    private boolean isAwardWinner; // true = Trophy/Cup, false = Just Nominated

    public enum AwardCategory {
        CHAMPION, RESEARCH, CORE_VALUES, ROBOT_DESIGN, COACHING, ROBOT_GAME
    }
}