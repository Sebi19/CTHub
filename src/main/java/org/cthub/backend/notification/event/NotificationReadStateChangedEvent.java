package org.cthub.backend.notification.event;

/**
 * Published whenever a user's read state actually changed, so other open tabs can refresh their
 * unread badge.
 */
public record NotificationReadStateChangedEvent(long userId) {
}
