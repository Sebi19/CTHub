package org.cthub.backend.notification.push;

/**
 * What the service worker ({@code frontend/public/sw.js}) displays. Serialized to JSON as the push
 * payload. {@code tag} groups notifications on the device: a newer one with the same tag replaces
 * the older one instead of piling up.
 *
 * @param notificationId the in-app notification to mark read on click; {@code null} for test pushes
 * @param url            app-relative path to open on click
 */
public record PushMessage(Long notificationId, String type, String title, String body, String url, String tag) {
}
