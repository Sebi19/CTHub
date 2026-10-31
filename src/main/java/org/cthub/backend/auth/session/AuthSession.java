package org.cthub.backend.auth.session;

import jakarta.persistence.*;
import lombok.*;
import org.cthub.backend.model.User;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * One signed-in browser/device. Lives up to {@link AuthSessionService#SESSION_LIFETIME} past its
 * last refresh and owns the device's push subscriptions.
 */
@Entity
@Table(name = "auth_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason", length = 32)
    private SessionRevokeReason revokeReason;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "auth_methods", nullable = false, length = 128)
    private String authMethods;

    @Column(name = "step_up_at")
    private Instant stepUpAt;

    public boolean isLive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public Set<AuthMethod> methods() {
        if (authMethods == null || authMethods.isBlank()) {
            return EnumSet.noneOf(AuthMethod.class);
        }
        return Arrays.stream(authMethods.split(","))
            .map(String::trim)
            .map(AuthMethod::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(AuthMethod.class)));
    }

    public static String encodeMethods(Set<AuthMethod> methods) {
        return methods.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }
}
