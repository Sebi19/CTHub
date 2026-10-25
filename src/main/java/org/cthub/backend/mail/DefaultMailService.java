package org.cthub.backend.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.outbox.MailOutboxService;
import org.cthub.backend.mail.render.MailRenderer;
import org.cthub.backend.mail.render.MailTemplateModel;
import org.cthub.backend.mail.spi.MailDeliveryException;
import org.cthub.backend.mail.spi.MailSender;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMailService implements MailService {

    private final MailRenderer mailRenderer;
    private final MailOutboxService outboxService;
    private final MailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void enqueue(MailTemplate template, MailAddress to, MailTemplateModel model, Locale locale) {
        enqueue(template, to, model, locale, null);
    }

    @Override
    public void enqueue(MailTemplate template, MailAddress to, MailTemplateModel model, Locale locale, String dedupeKey) {
        if (!mailProperties.isEnabled()) {
            log.debug("Mail is disabled (app.mail.enabled=false); skipping enqueue of {} to {}.", template, to.email());
            return;
        }
        MailMessage rendered = mailRenderer.render(template, model, locale, to);
        outboxService.enqueue(rendered, template, locale, dedupeKey);
    }

    @Override
    public MailSendResult sendNow(MailTemplate template, MailAddress to, MailTemplateModel model, Locale locale)
        throws MailDeliveryException {
        MailMessage rendered = mailRenderer.render(template, model, locale, to);
        MailAddress from = new MailAddress(mailProperties.getFrom().getAddress(), mailProperties.getFrom().getName());
        String replyToEmail = mailProperties.getReplyTo();
        MailAddress replyTo = replyToEmail != null && !replyToEmail.isBlank() ? MailAddress.of(replyToEmail) : null;
        return mailSender.send(rendered.withEnvelope(from, replyTo));
    }
}
