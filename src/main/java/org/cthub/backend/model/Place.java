package org.cthub.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "place_seq_gen")
    @SequenceGenerator(name = "place_seq_gen", sequenceName = "place_seq")
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private SeasonTeam seasonTeam;

    @NotNull
    @ManyToOne(optional = false)
    private Competition competition;

    @NotNull
    @Column(nullable = false)
    private int place; // 1st, 2nd, 3rd overall

    @NotNull
    @Column(nullable = false)
    private boolean advancing;
}