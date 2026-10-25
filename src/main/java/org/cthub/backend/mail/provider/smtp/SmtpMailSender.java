package org.cthub.backend.mail.provider.smtp;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailAttachment;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.spi.MailDeliveryException;
import org.cthub.backend.mail.spi.MailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Sends mail via plain SMTP using the already-present {@code spring-boot-starter-mail} JavaMailSender.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public MailSendResult send(MailMessage message) throws MailDeliveryException {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(toInternetAddress(message.from()));
            helper.setTo(toInternetAddress(message.to()));
            if (message.replyTo() != null) {
                helper.setReplyTo(toInternetAddress(message.replyTo()));
            }
            for (MailAddress cc : message.cc()) {
                helper.addCc(toInternetAddress(cc));
            }
            for (MailAddress bcc : message.bcc()) {
                helper.addBcc(toInternetAddress(bcc));
            }
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());

            for (MailAttachment attachment : message.attachments()) {
                if (attachment.isInline()) {
                    helper.addInline(attachment.contentId(), new ByteArrayResource(attachment.content()), attachment.contentType());
                } else {
                    helper.addAttachment(attachment.filename(), new ByteArrayResource(attachment.content()), attachment.contentType());
                }
            }

            javaMailSender.send(mimeMessage);
            return new MailSendResult(UUID.randomUUID().toString(), Instant.now());
        } catch (MailException e) {
            throw new MailDeliveryException("SMTP send failed: " + e.getMessage(), true, null, e);
        } catch (Exception e) {
            throw new MailDeliveryException("SMTP send failed: " + e.getMessage(), false, null, e);
        }
    }

    private String toInternetAddress(MailAddress address) {
        if (address.name() == null || address.name().isBlank()) {
            return address.email();
        }
        return "%s <%s>".formatted(address.name(), address.email());
    }

    @Override
    public String providerId() {
        return "smtp";
    }
}
