package org.cthub.backend.auth.attempt;

/** Why a browser has to prove the account owner is present. */
public enum LoginAttemptPurpose {
    /** Sign in (and, for a new address, create the account on approval). */
    LOGIN,
    /** Re-verify an already signed-in session before a sensitive change. */
    STEP_UP
}
