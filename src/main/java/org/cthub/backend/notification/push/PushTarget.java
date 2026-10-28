package org.cthub.backend.notification.push;

/** Everything needed to deliver to one device; a projection of {@link PushSubscription}. */
public record PushTarget(long id, long userId, String endpoint, String p256dh, String auth) {
}
