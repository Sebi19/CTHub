package org.cthub.backend.mail.spi;

import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailSendResult;

/**
 * Port implemented by one adapter per mail provider (ZeptoMail, SMTP, local-dev logging, ...).
 * Business code never depends on this interface directly; go through
 * {@link org.cthub.backend.mail.MailService} instead.
 */
public interface MailSender {

    MailSendResult send(MailMessage message) throws MailDeliveryException;

    /** Short id used in {@code app.mail.provider} and stored on outbox entries, e.g. "zeptomail". */
    String providerId();
}
