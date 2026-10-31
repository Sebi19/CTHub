package org.cthub.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single-use, hashed token mailed to a user (currently: e-mail change confirmations). The raw token is never
 * stored - only its SHA-256 hash (see {@code TokenService}).
 */
@Entity
@Table(name = "user_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_token_seq_gen")
    @SequenceGenerator(name = "user_token_seq_gen", sequenceName = "user_token_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserTokenType type;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Type-specific data, e.g. the new address for {@link UserTokenType#EMAIL_CHANGE}. */
    @Column(length = 255)
    private String payload;

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isUsable() {
        return !isConsumed() && !isExpired();
    }
}
