package org.cthub.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single-use, hashed token used for e-mail verification or password reset. The raw token is never
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

    /**
     * Correct answer to the cross-device confirmation challenge, 10-99.
     *
     * <p>The {@code challenge*} fields describe a registration attempt and are only ever populated
     * on {@link UserTokenType#REGISTRATION_STATUS} rows - that token is the attempt record, held by
     * the browser that started the registration. They stay {@code null} on every other token type.
     */
    @Column(name = "challenge_number")
    private Short challengeNumber;

    /** Number of wrong picks so far. Each one burns a verification link; {@code 3} cancels the attempt. */
    @Column(name = "challenge_attempts", nullable = false)
    @Builder.Default
    private short challengeAttempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_outcome", length = 32)
    private RegistrationOutcome challengeOutcome;

    /** Set the first time the e-mailed link is opened, so the waiting browser can say "check your other device". */
    @Column(name = "challenge_opened_at")
    private Instant challengeOpenedAt;

    public boolean isChallengeSettled() {
        return challengeOutcome != null;
    }

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
