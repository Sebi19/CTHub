package org.cthub.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonTeamProfile {
    @Id
    private Long id;

    @NotNull
    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "season_team_id")
    private SeasonTeam seasonTeam;

    @NotNull
    @ManyToOne(optional = false)
    private TeamProfile teamProfile;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private AvatarMode avatarMode = AvatarMode.INHERIT;

    private UUID customAvatarId;

    public enum AvatarMode {
        INHERIT, // Default: Use the global Team profile image
        HIDE,    // No image: Force the fallback/placeholder for this season
        CUSTOM   // Alternative: Use the custom_avatar_id for this season
    }
}
