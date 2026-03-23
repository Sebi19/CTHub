package org.cthub.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    @ManyToOne(optional = false)
    private Season season;

    @NotNull
    @Column(nullable = false)
    private boolean active;

    @NotNull
    @Column(nullable = false)
    private String fllId; // "1011" - Not unique globally!

    @NotNull
    @Column(nullable = false)
    private String name; // Name used IN THIS SEASON
    private String institution;
    private String city;
    private String country; // "AT"

    @OneToOne(mappedBy = "seasonTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SeasonTeamProfile seasonTeamProfile;

    @ElementCollection
    @Builder.Default
    private List<Link> links = new ArrayList<>();

    @ManyToMany
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Competition> registeredCompetitions = new HashSet<>();
}