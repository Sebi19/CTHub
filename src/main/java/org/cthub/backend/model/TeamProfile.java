package org.cthub.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

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

    @NotNull
    @Column(unique = true, nullable = false)
    private String customUrl; // "rootbots" - The PERMANENT identifier

    @NotNull
    @Column(nullable = false)
    private String displayName; // "ROOTBOTS"

    private UUID profileImageUuid;
}