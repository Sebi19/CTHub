package org.cthub.backend.mail.render;

/**
 * Model for the {@code security-alert} template.
 *
 * @param device "Browser on OS" of the device that made the change, or {@code null} if unknown
 */
public record SecurityAlertModel(String firstName, Kind kind, String device) implements MailTemplateModel {

    /** What happened; each kind has a {@code mail.securityAlert.<KIND>} message. */
    public enum Kind {
        PASSWORD_SET,
        PASSWORD_CHANGED,
        PASSWORD_REMOVED,
        TOTP_ENABLED,
        TOTP_DISABLED,
        PASSKEY_ADDED,
        PASSKEY_REMOVED,
        /** Sent to the old address after the account moved to a new one. */
        EMAIL_CHANGED,
        /** A stolen refresh token was replayed; the affected session was signed out. */
        SESSION_REUSE_DETECTED
    }
}
