package org.cthub.backend.model;

import jakarta.persistence.*;
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

    @ManyToOne(optional = false)
    private SeasonTeam seasonTeam;

    @ManyToOne(optional = false)
    private Competition competition;

    @Enumerated(EnumType.STRING)
    private AwardCategory category;

    private boolean isAwardWinner; // true = Trophy/Cup, false = Just Nominated

    public enum AwardCategory {
        CHAMPION, RESEARCH, CORE_VALUES, ROBOT_DESIGN, COACHING, ROBOT_GAME
    }
}