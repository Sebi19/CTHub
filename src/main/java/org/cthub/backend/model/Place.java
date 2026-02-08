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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private SeasonTeam seasonTeam;

    @ManyToOne(optional = false)
    private Competition competition;

    private int place; // 1st, 2nd, 3rd overall

    private boolean advancing;
}