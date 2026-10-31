package org.cthub.backend.auth.attempt;

/**
 * Stored lifecycle of a {@link LoginAttempt}. "Expired" is never stored; it is derived from
 * {@code expiresAt} (see {@link LoginAttempt#isPending}).
 */
public enum LoginAttemptStatus {
    PENDING,
    /** The owner picked the right number elsewhere; the initiating browser may now complete. */
    APPROVED,
    /** The initiating browser was signed in (or stepped up). Terminal. */
    COMPLETED,
    /** The owner chose to sign in on the browser that opened the link instead. Terminal. */
    CLAIMED_ELSEWHERE,
    /** Wrong number or "this wasn't me". Terminal on every device. */
    REJECTED,
    /** Abandoned by the initiating browser, or the account turned out to be disabled. Terminal. */
    CANCELLED
}
