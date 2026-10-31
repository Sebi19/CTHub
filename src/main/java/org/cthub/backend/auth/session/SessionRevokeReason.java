package org.cthub.backend.auth.session;

public enum SessionRevokeReason {
    /** The user signed out on this device. */
    LOGOUT,
    /** The user ended it from another device ("sign out other sessions"). */
    USER_REVOKED,
    /** An already-rotated refresh token was presented again: the token was most likely stolen. */
    REUSE_DETECTED,
    /** The same browser signed in again (possibly as someone else). */
    REPLACED,
    /** The password changed; every other session that may have relied on the old one ends. */
    CREDENTIALS_CHANGED,
    /** The account was disabled or deleted. */
    ACCOUNT_DISABLED
}
