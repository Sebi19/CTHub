package org.cthub.backend.mail.render;

import java.time.Duration;

/** Model for the {@code email-change} template, sent to the <em>new</em> address. */
public record EmailChangeModel(String firstName, String newEmail, String confirmUrl, Duration validity)
    implements MailTemplateModel {
}
