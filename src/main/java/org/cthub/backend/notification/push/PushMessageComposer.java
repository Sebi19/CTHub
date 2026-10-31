package org.cthub.backend.notification.push;

import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Turns a notification into what the device shows. The switch is exhaustive on purpose: adding a
 * {@link org.cthub.backend.notification.model.NotificationType} forces a decision about its push text.
 */
@Component
public class PushMessageComposer {

    /** Frontend route opened when the notification itself has no link. */
    static final String NOTIFICATIONS_PATH = "/account/notifications";
    static final String TEST_TYPE = "TEST";
    private static final int MAX_TITLE = 120;
    private static final int MAX_BODY = 600;

    public PushMessage compose(NotificationsCreatedEvent.Item item) {
        Map<String, String> params = item.params();
        String url = item.link() != null ? item.link() : NOTIFICATIONS_PATH;
        String tag = tag(item.type().name());

        return switch (item.type()) {
            case ANNOUNCEMENT -> new PushMessage(
                item.id(), item.type().name(), truncate(params.get("title"), MAX_TITLE),
                truncate(params.get("body"), MAX_BODY), url, tag);
            case WELCOME -> throw new IllegalStateException(
                "WELCOME notifications are not pushed; no device can be registered before the profile is complete.");
        };
    }

    /** "Does push work on my device?" — not backed by an in-app notification. */
    public PushMessage test(Locale locale) {
        boolean german = locale == null || "de".equals(locale.getLanguage());
        return new PushMessage(
            null,
            TEST_TYPE,
            german ? "Testbenachrichtigung" : "Test notification",
            german ? "Push-Benachrichtigungen funktionieren auf diesem Gerät." : "Push notifications work on this device.",
            NOTIFICATIONS_PATH + "/settings",
            tag(TEST_TYPE));
    }

    /** Also used as the RFC 8030 {@code Topic}, so it must stay within [A-Za-z0-9_-]{1,32}. */
    static String tag(String type) {
        return "cthub-" + type;
    }

    static final String LOGIN_APPROVAL_TYPE = "LOGIN_APPROVAL";
    static final String DISMISS_TYPE = "DISMISS";

    /**
     * Asks a signed-in device to approve a login elsewhere. Opens the approval screen, which shows
     * the number choice; the push itself never carries the number.
     *
     * @param tag also the RFC 8030 topic, unique per login attempt
     */
    public PushMessage loginApproval(Locale locale, UUID attemptId, String deviceLabel, String tag) {
        boolean german = locale == null || "de".equals(locale.getLanguage());
        String body;
        if (deviceLabel != null) {
            body = german
                ? "Anmeldeversuch über " + deviceLabel + ". Tippe, um ihn zu bestätigen."
                : "Sign-in attempt from " + deviceLabel + ". Tap to confirm it.";
        } else {
            body = german ? "Tippe, um den Anmeldeversuch zu bestätigen." : "Tap to confirm the sign-in attempt.";
        }
        return new PushMessage(
            null,
            LOGIN_APPROVAL_TYPE,
            german ? "Bist du das?" : "Is this you?",
            body,
            "/auth/approve/" + attemptId,
            tag);
    }

    /** Tells the service worker to close the notification with this tag, without showing anything. */
    static PushMessage dismiss(String tag) {
        return new PushMessage(null, DISMISS_TYPE, null, null, null, tag);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String flat = text.strip();
        return flat.length() <= max ? flat : flat.substring(0, max - 1).stripTrailing() + "…";
    }
}
