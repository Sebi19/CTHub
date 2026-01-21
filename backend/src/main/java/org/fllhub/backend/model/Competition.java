package org.fllhub.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Season season;

    private String name; // "Weinviertel"
    private String country; // "at"
    private LocalDate date; // "2026-01-16"

    @Enumerated(EnumType.STRING)
    private CompetitionType type; // Enum defined below

    @Column(unique = true)
    private String urlPart; // "weinviertel"

    // Can actually differ from normal url part
    @Column(unique = true)
    private String resultsUrlPart; // "weinviertel"

    private boolean resultsAvailable;

    @Column(columnDefinition = "TEXT")
    private String location; // "HTL Hollabrunn"

    // Stores "Link 1", "Link 2" in a separate simple table automatically
    @ElementCollection
    private List<Link> links;

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