package org.cthub.backend.mail.render;

import java.time.Duration;

/**
 * Model for the {@code login-link} template: the e-mailed half of a login or step-up challenge.
 * {@code firstName} is {@code null} for addresses without a (completed) account.
 */
public record LoginLinkModel(String firstName, String loginUrl, Duration validity) implements MailTemplateModel {
}
