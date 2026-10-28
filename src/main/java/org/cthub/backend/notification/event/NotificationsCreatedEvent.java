package org.cthub.backend.notification.event;

import org.cthub.backend.notification.model.Notification;
import org.cthub.backend.notification.model.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Published inside the transaction that created the notifications. Delivery channels react to it:
 * durable ones (e-mail outbox) before commit, best-effort ones (live stream) after commit.
 * Carries immutable snapshots so listeners never touch detached entities.
 */
public record NotificationsCreatedEvent(List<Item> notifications) {

    public NotificationsCreatedEvent {
        notifications = List.copyOf(notifications);
    }

    public static NotificationsCreatedEvent of(List<Notification> created) {
        return new NotificationsCreatedEvent(created.stream().map(Item::of).toList());
    }

    public record Item(
        long id,
        long userId,
        NotificationType type,
        Map<String, String> params,
        String link,
        Instant createdAt
    ) {
        static Item of(Notification n) {
            return new Item(n.getId(), n.getUser().getId(), n.getType(), n.getParams(), n.getLink(), n.getCreatedAt());
        }
    }
}
