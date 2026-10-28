package org.cthub.backend.notification.model;

/**
 * Optional delivery channels a user can switch on or off per {@link NotificationType}. The in-app
 * list (and its live stream) is not a channel in this sense: it is always on. Stored by name in
 * {@code notification_preference.channel}, so never rename or remove a value.
 */
public enum NotificationChannel {
    EMAIL,
    /** Web Push to every device the user registered (see {@code push_subscription}). */
    PUSH
}
