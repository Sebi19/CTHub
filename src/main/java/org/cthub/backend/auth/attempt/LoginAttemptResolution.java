package org.cthub.backend.auth.attempt;

/** How a {@link LoginAttempt} was resolved. */
public enum LoginAttemptResolution {
    /** The e-mailed link was opened in the initiating browser itself. */
    SAME_BROWSER,
    /** The right number was picked on the page behind the e-mailed link, in another browser. */
    EMAIL,
    /** The right number was picked on a signed-in device after a push. */
    PUSH,
    /** The link's browser signed itself in instead of the initiating one. */
    CLAIMED_HERE
}
