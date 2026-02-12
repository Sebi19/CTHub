package org.cthub.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_seq_gen")
    @SequenceGenerator(name = "team_seq_gen", sequenceName = "team_seq")
    private Long id;

    @ManyToOne(optional = false)
    private Season season;

    private boolean active;

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
    @Builder.Default
    private List<Link> links = new ArrayList<>();

    @ManyToMany
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Competition> registeredCompetitions = new HashSet<>();
}