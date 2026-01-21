package org.fllhub.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
// The combination of Season + FLL ID must be unique
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "season_id", "fllId" }) })
public class SeasonTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Season season;

    @Column(nullable = false)
    private String fllId; // "1011" - Not unique globally!

    private String name; // Name used IN THIS SEASON
    private String institution;
    private String city;
    private String country; // "AT"

    // Link to the "Brand". Null when first scraped.
    @ManyToOne
    private TeamProfile teamProfile;

    @ElementCollection
    private List<Link> links;

    @ManyToMany
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Competition> registeredCompetitions;
}