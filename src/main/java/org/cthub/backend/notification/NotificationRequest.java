package org.cthub.backend.notification;

import lombok.Builder;
import org.cthub.backend.notification.model.NotificationType;

import java.util.Map;
import java.util.Objects;

/**
 * What to notify about, independent of who receives it.
 *
 * @param link      in-app path (must start with a single {@code /}). External or scheme URLs are
 *                  rejected: the link ends up as a click target in toasts, mails and push messages.
 * @param dedupeKey optional, permanent per recipient - see {@code V13__add_notifications.sql}.
 */
@Builder
public record NotificationRequest(
    NotificationType type,
    Map<String, String> params,
    String link,
    String dedupeKey
) {

    public NotificationRequest {
        Objects.requireNonNull(type, "type");
        params = params == null ? Map.of() : Map.copyOf(params);
        if (link != null && !isInternalPath(link)) {
            throw new IllegalArgumentException("Notification link must be an in-app path: " + link);
        }
    }

    public static NotificationRequest of(NotificationType type) {
        return new NotificationRequest(type, null, null, null);
    }

    private static boolean isInternalPath(String link) {
        return link.startsWith("/") && !link.startsWith("//") && !link.contains("\\");
    }
}
