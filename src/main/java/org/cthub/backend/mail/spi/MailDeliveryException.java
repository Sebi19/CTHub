package org.cthub.backend.mail.spi;

import lombok.Getter;

/**
 * Raised by a {@link MailSender} when a provider rejects or fails to accept a message.
 */
@Getter
public class MailDeliveryException extends Exception {

    /** Whether retrying the send later has a realistic chance of succeeding. */
    private final boolean retryable;

    /** Provider-specific error code, if any (e.g. HTTP status or vendor error id), for diagnostics. */
    private final String providerErrorCode;

    public MailDeliveryException(String message, boolean retryable, String providerErrorCode, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
        this.providerErrorCode = providerErrorCode;
    }

    public MailDeliveryException(String message, boolean retryable, String providerErrorCode) {
        this(message, retryable, providerErrorCode, null);
    }
}
