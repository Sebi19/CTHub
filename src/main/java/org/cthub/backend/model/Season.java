package org.cthub.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {
    @Id
    private String id; // "2025-2026"

    @NotNull
    @Column(nullable = false)
    private String name; // "Unearthed"

    @NotNull
    @Column(unique = true, nullable = false)
    private Integer startYear; // 2025

    // only true once
    @NotNull
    @Column(nullable = false)
    private boolean active;

    private String overviewHash;

    private Integer maxPoints;
}