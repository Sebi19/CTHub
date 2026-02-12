package org.cthub.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = { "season_id", "url_part" })
})
public class Competition {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comp_seq_gen")
    @SequenceGenerator(name = "comp_seq_gen", sequenceName = "comp_seq")
    private Long id;

    @ManyToOne(optional = false)
    private Season season;

    private boolean active;

    private String name; // "Weinviertel"
    private String country; // "at"
    private LocalDate date; // "2026-01-16"

    @Enumerated(EnumType.STRING)
    private CompetitionType type; // Enum defined below

    private int registeredTeamCount; // 12
    private int maxTeamCount; // 24

    private String urlPart; // "weinviertel"

    // Can actually differ from normal url part
    private String resultsUrlPart; // "weinviertel"

    private String qualificationUrlPart; // "weinviertel-qualifikation"

    private boolean resultsAvailable = false;

    @Column(columnDefinition = "TEXT")
    private String location; // "HTL Hollabrunn"

    // Stores "Link 1", "Link 2" in a separate simple table automatically
    @ElementCollection
    @Builder.Default
    private List<Link> links = new ArrayList<>();

    // Embed contact info directly into this table to avoid unnecessary joins
    @Embedded
    private ContactInfo contact;

    public enum CompetitionType {
        REGIONAL, QUALIFICATION, FINAL
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactInfo {
        @Column(columnDefinition = "TEXT")
        private String contactName;
        private String contactEmail;
    }

    private String detailHash;

    private String robotGameHash;

    private String awardsHash;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Competition that = (Competition) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}