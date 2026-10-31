package org.cthub.backend.dto.auth;

/** Browser A's view of its pending challenge. */
public enum LoginAttemptState {
    WAITING,
    /** Approved elsewhere; call {@code complete} to pick up the session. */
    APPROVED,
    COMPLETED,
    /** A wrong number was picked or the request was declined. */
    REJECTED,
    CANCELLED,
    /** "Sign in on this browser instead" was used on the device holding the link. */
    CLAIMED_ELSEWHERE,
    EXPIRED
}