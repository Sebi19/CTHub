package org.cthub.backend.mail.model;

import java.time.Instant;

/**
 * Result of a successful hand-off of a message to a provider.
 */
public record MailSendResult(String providerMessageId, Instant acceptedAt) {
}
