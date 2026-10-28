package org.cthub.backend.notification.push;

import jakarta.persistence.*;
import lombok.*;
import org.cthub.backend.model.User;

import java.time.Instant;

/** One browser/device that receives Web Push notifications for {@link #user}. */
@Entity
@Table(name = "push_subscription")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "push_subscription_seq_gen")
    @SequenceGenerator(name = "push_subscription_seq_gen", sequenceName = "push_subscription_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Push service URL; a capability, so it is never exposed through the API. */
    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String endpoint;

    /** The browser's P-256 public key, base64url (uncompressed point). */
    @Column(nullable = false, length = 128)
    private String p256dh;

    /** The browser's 16-byte authentication secret, base64url. */
    @Column(nullable = false, length = 64)
    private String auth;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Whether the subscription came from the installed app (standalone display mode). */
    @Column(nullable = false)
    private boolean installed;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Last time the device confirmed the subscription (it re-syncs on every app start). */
    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private Instant lastSeenAt = Instant.now();
}
