package org.cthub.backend.mail.render;

/**
 * Model for the {@code welcome} template, sent right after e-mail verification succeeds.
 */
public record WelcomeModel(String firstName) implements MailTemplateModel {
}
