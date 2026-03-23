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
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Generates a standard UUID string like "550e8400-e29b-..."

    @NotNull
    @Column(nullable = false)
    private String contentType; // e.g., "image/jpeg" or "image/png"

    private String originalFilename;

    private Long sizeBytes;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] data;
}