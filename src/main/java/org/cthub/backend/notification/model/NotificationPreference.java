package org.cthub.backend.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.cthub.backend.model.User;

import java.time.Instant;

/** A user's explicit choice for one {@link NotificationType} on one {@link NotificationChannel}. */
@Entity
@Table(name = "notification_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_preference_seq_gen")
    @SequenceGenerator(name = "notification_preference_seq_gen", sequenceName = "notification_preference_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
