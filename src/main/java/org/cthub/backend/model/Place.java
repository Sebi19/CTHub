package org.cthub.backend.model;

import jakarta.persistence.*;
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

    @ManyToOne(optional = false)
    private SeasonTeam seasonTeam;

    @ManyToOne(optional = false)
    private Competition competition;

    private int place; // 1st, 2nd, 3rd overall

    private boolean advancing;
}