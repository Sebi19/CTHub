package org.cthub.backend.mail.model;

import java.util.List;
import java.util.Map;

/**
 * A fully rendered, provider-agnostic mail message ready to be sent (or stored in the outbox).
 */
public record MailMessage(
    MailAddress from,
    MailAddress to,
    List<MailAddress> cc,
    List<MailAddress> bcc,
    MailAddress replyTo,
    String subject,
    String htmlBody,
    String textBody,
    List<MailAttachment> attachments,
    Map<String, String> headers,
    List<String> tags
) {

    public MailMessage {
        cc = cc == null ? List.of() : List.copyOf(cc);
        bcc = bcc == null ? List.of() : List.copyOf(bcc);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    /** Returns a copy with the sender envelope (from/reply-to) filled in, keeping the rendered content. */
    public MailMessage withEnvelope(MailAddress from, MailAddress replyTo) {
        return new MailMessage(from, to, cc, bcc, replyTo, subject, htmlBody, textBody, attachments, headers, tags);
    }
}
