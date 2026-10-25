package org.cthub.backend.mail.render;

import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailTemplate;

import java.util.Locale;

/**
 * Renders a {@link MailTemplate} + typed model into a fully formed {@link MailMessage} (subject,
 * HTML body and plain-text body), in a given locale, addressed to the given recipient.
 */
public interface MailRenderer {

    MailMessage render(MailTemplate template, MailTemplateModel model, Locale locale, MailAddress recipient);
}
