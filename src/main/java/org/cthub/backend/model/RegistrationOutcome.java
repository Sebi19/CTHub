package org.cthub.backend.model;

/**
 * Terminal outcome of a registration attempt's cross-device confirmation challenge, as observed by
 * the browser that started the registration.
 */
public enum RegistrationOutcome {

    /** The correct number was picked: the originating browser may claim the session. */
    CONFIRMED,

    /** Whoever opened the link chose to continue there instead; the originating browser must stop. */
    CLAIMED_ELSEWHERE,

    /** Rejected ("that wasn't me") or too many wrong picks. The attempt is dead. */
    CANCELLED
}
