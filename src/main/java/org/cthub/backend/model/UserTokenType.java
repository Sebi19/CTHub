package org.cthub.backend.model;

/**
 * Purpose of a {@link UserToken}.
 */
public enum UserTokenType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    /** Single-use passwordless sign-in link, for accounts that never set a password. */
    LOGIN_LINK,
    /**
     * Opaque handle returned to the browser that started a registration so the pending page can
     * poll for completion. Deliberately never e-mailed: it proves "I started this registration",
     * not "I own this inbox", so on its own it can never verify an account.
     */
    REGISTRATION_STATUS
}
