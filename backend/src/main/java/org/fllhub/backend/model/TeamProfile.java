package org.fllhub.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String customUrl; // "rootbots" - The PERMANENT identifier

    private String displayName; // "ROOTBOTS"

    // One Profile has many seasonal participations
    @OneToMany(mappedBy = "teamProfile")
    private List<SeasonTeam> participations;
}