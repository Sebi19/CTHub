package org.cthub.backend.dto.auth;

public enum ChallengePickResult {
    /** Correct number: the initiating browser is now being signed in / verified. */
    APPROVED,
    /** Wrong number: the request was cancelled everywhere. */
    REJECTED
}