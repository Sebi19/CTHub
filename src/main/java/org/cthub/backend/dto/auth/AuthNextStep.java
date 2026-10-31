package org.cthub.backend.dto.auth;

/** What the sign-in screen shows after the e-mail was submitted. */
public enum AuthNextStep {
    /** The account has passkeys: offer them first. */
    PASSKEY,
    /** The account opted into a password (and maybe TOTP). */
    PASSWORD,
    /** Number-matching challenge started (push and/or e-mailed link). */
    CHALLENGE
}