package org.cthub.backend.notification;

import org.cthub.backend.notification.model.Notification;

import java.util.List;

/** One keyset page of notifications; {@code nextCursor} is {@code null} on the last page. */
public record NotificationPage(List<Notification> items, Long nextCursor) {
}
