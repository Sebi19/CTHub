package org.cthub.backend.dto.auth;

public enum ChallengeViewKind {
    /** Opened in the browser that started it: completing needs no number. */
    SAME_BROWSER,
    /** Opened elsewhere: approve by number, or (for sign-ins via link) sign in here instead. */
    CROSS,
    /** Invalid, used, or expired. */
    INVALID
}