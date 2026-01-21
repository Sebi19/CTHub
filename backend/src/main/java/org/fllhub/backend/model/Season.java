package org.fllhub.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {
    @Id
    private String id; // "2025-2026"

    private String name; // "Unearthed"

    @Column(unique = true)
    private Integer startYear; // 2025

    // only true once
    @Column
    private boolean active;
}