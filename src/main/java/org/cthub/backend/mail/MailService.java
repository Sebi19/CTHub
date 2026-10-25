package org.cthub.backend.mail;

import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.render.MailTemplateModel;
import org.cthub.backend.mail.spi.MailDeliveryException;

import java.util.Locale;

/**
 * The only thing business code should touch to send mail. Queues a rendered message into the
 * transactional outbox (see {@link org.cthub.backend.mail.outbox.MailOutboxService}), to be
 * dispatched asynchronously by {@link org.cthub.backend.mail.outbox.MailDispatcher}.
 */
public interface MailService {

    void enqueue(MailTemplate template, MailAddress to, MailTemplateModel model, Locale locale);

    /**
     * Same as {@link #enqueue(MailTemplate, MailAddress, MailTemplateModel, Locale)}, but with a
     * {@code dedupeKey} that prevents duplicate sends (e.g. a double-clicked "resend confirmation").
     */
    void enqueue(MailTemplate template, MailAddress to, MailTemplateModel model, Locale locale, String dedupeKey);

    /**
     * Escape hatch that renders and sends immediately, bypassing the outbox entirely. Rarely used -
     * e.g. for an admin "send test mail" action. Prefer {@link #enqueue} for anything user-facing.
     */
    MailSendResult sendNow(MailTemplate template, MailAddress to, MailTemplateModel model, Locale locale)
        throws MailDeliveryException;
}
