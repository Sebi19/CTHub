package org.cthub.backend.mail.model;

/**
 * Broad classification of outbound mail, used for rate limiting and (later) user preferences.
 */
public enum MailCategory {
    /** Required for the account/transaction to function (e.g. e-mail confirmation). Never opt-out. */
    TRANSACTIONAL,
    /** Informational, opt-out-able notifications (e.g. "your team was nominated"). */
    NOTIFICATION,
    /** Periodic batched summaries. */
    DIGEST
}
