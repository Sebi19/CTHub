package org.cthub.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_seq_gen")
    @SequenceGenerator(name = "profile_seq_gen", sequenceName = "profile_seq")
    private Long id;

    @Column(unique = true, nullable = false)
    private String customUrl; // "rootbots" - The PERMANENT identifier

    private String displayName; // "ROOTBOTS"
}