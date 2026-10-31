package org.cthub.backend.auth.attempt;

import jakarta.persistence.*;
import lombok.*;
import org.cthub.backend.model.User;

import java.time.Instant;
import java.util.UUID;

/**
 * One "prove it's you" attempt started by a browser ("browser A"), which is bound to it by the
 * nonce in its attempt cookie. Approved via push on a signed-in device or via the e-mailed link.
 * {@code user} stays {@code null} for an address without an account until the attempt is approved.
 */
@Entity
@Table(name = "login_attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 16)
    private LoginAttemptPurpose purpose;

    @Column(name = "email", nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** For {@link LoginAttemptPurpose#STEP_UP}: the session that is being re-verified. */
    @Column(name = "step_up_session_id")
    private UUID stepUpSessionId;

    @Column(name = "nonce_hash", nullable = false, unique = true, length = 64)
    private String nonceHash;

    /** Hash of the token in the most recently e-mailed link; older links stop working. */
    @Column(name = "link_token_hash", unique = true, length = 64)
    private String linkTokenHash;

    @Column(name = "challenge_number", nullable = false)
    private short challengeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LoginAttemptStatus status;

    @Column(name = "locale", length = 16)
    private String locale;

    @Column(name = "initiator_user_agent", length = 512)
    private String initiatorUserAgent;

    @Column(name = "initiator_ip", length = 64)
    private String initiatorIp;

    @Column(name = "excluded_push_endpoint", columnDefinition = "text")
    private String excludedPushEndpoint;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    @Column(name = "push_opened_at")
    private Instant pushOpenedAt;

    /** When the link mail is due; {@code null} once sent or once a push was opened. */
    @Column(name = "email_due_at")
    private Instant emailDueAt;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "link_opened_at")
    private Instant linkOpenedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_via", length = 16)
    private LoginAttemptResolution resolvedVia;

    @Column(name = "resolved_by_session_id")
    private UUID resolvedBySessionId;

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    /** {@code true} while the owner can still approve or reject it. */
    public boolean isPending(Instant now) {
        return status == LoginAttemptStatus.PENDING && !isExpired(now);
    }

    /** Push/approval tag and RFC 8030 topic: unique per attempt, within [A-Za-z0-9_-]{1,32}. */
    public String pushTag() {
        return id.toString().replace("-", "");
    }
}
