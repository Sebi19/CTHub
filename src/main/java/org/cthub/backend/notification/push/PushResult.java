package org.cthub.backend.notification.push;

/** Outcome of one push request. */
public enum PushResult {
    /** Accepted by the push service (2xx). */
    DELIVERED,
    /** The subscription no longer exists (404/410) or its keys are unusable; delete it. */
    GONE,
    /** Rejected for a reason a retry won't fix (other 4xx). The subscription may still be fine. */
    REJECTED,
    /** Transient: network error, 429 or 5xx. */
    FAILED
}
