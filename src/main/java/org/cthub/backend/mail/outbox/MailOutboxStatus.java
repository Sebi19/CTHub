package org.cthub.backend.mail.outbox;

/**
 * Lifecycle status of a {@link MailOutboxEntry}.
 */
public enum MailOutboxStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    CANCELLED
}
