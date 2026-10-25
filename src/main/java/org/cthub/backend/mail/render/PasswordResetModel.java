package org.cthub.backend.mail.render;

import java.time.Duration;

/**
 * Model for the {@code password-reset} template.
 */
public record PasswordResetModel(String firstName, String resetUrl, Duration validity) implements MailTemplateModel {
}
