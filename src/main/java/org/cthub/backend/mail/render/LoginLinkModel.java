package org.cthub.backend.mail.render;

import java.time.Duration;

/**
 * Model for the {@code login-link} template. {@code firstName} may be {@code null} for accounts
 * that never completed their profile.
 */
public record LoginLinkModel(String firstName, String loginUrl, Duration validity) implements MailTemplateModel {
}
