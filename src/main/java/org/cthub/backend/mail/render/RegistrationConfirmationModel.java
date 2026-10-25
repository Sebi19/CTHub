package org.cthub.backend.mail.render;

import java.time.Duration;

/**
 * Model for the {@code registration-confirmation} template.
 */
public record RegistrationConfirmationModel(String firstName, String confirmUrl, Duration validity) implements MailTemplateModel {
}
