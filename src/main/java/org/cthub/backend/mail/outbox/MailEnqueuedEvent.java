package org.cthub.backend.mail.outbox;

/**
 * Published when a mail is queued, so {@link MailDispatcher} can send it right after the enqueuing
 * transaction commits instead of waiting for the next poll (login links are time-critical).
 */
public record MailEnqueuedEvent(Long outboxId) {
}
