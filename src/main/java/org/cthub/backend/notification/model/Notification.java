package org.cthub.backend.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.cthub.backend.model.User;

import java.time.Instant;
import java.util.Map;

/**
 * A notification delivered to a single user. This row is the source of truth for the in-app list
 * and unread badge; the other channels (live stream, e-mail, push) only ever deliver copies of it.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq_gen")
    @SequenceGenerator(name = "notification_seq_gen", sequenceName = "notification_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    /** Interpolation values for the type's message (e.g. team or competition name). */
    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "text")
    @Builder.Default
    private Map<String, String> params = Map.of();

    /** In-app path the notification points to, e.g. {@code /competition/2025-26/berlin}. */
    @Column(length = 2048)
    private String link;

    @Column(name = "dedupe_key")
    private String dedupeKey;

    // Set explicitly rather than via @CreationTimestamp so the value is known before the insert is
    // flushed - the created-event snapshot needs it.
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "read_at")
    private Instant readAt;

    public boolean isRead() {
        return readAt != null;
    }
}
