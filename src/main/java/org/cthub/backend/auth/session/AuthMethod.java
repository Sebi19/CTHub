package org.cthub.backend.auth.session;

/** How a user proved their identity when a session was established (or stepped up). */
public enum AuthMethod {
    PASSKEY,
    PASSWORD,
    TOTP,
    /** Approved via the e-mailed link (same browser, number match, or "sign in here"). */
    EMAIL,
    /** Approved from a push notification on another signed-in device. */
    PUSH
}
