package org.cthub.backend.mail.model;

/**
 * Registry of all mail templates owned by this repo. {@code templatePath} is resolved relative to
 * {@code mail/templates/} (without extension; the renderer appends {@code .html}/{@code .txt}).
 * {@code subjectKey} is looked up in the {@code mail_<locale>.properties} bundles.
 */
public enum MailTemplate {
    REGISTRATION_CONFIRMATION("registration-confirmation", "mail.registrationConfirmation.subject", MailCategory.TRANSACTIONAL),
    WELCOME("welcome", "mail.welcome.subject", MailCategory.TRANSACTIONAL),
    PASSWORD_RESET("password-reset", "mail.passwordReset.subject", MailCategory.TRANSACTIONAL),
    LOGIN_LINK("login-link", "mail.loginLink.subject", MailCategory.TRANSACTIONAL),
    /** Any opted-in in-app notification, see {@code EmailNotificationChannel}. */
    NOTIFICATION("notification", "mail.notification.subject", MailCategory.NOTIFICATION);

    private final String templatePath;
    private final String subjectKey;
    private final MailCategory category;

    MailTemplate(String templatePath, String subjectKey, MailCategory category) {
        this.templatePath = templatePath;
        this.subjectKey = subjectKey;
        this.category = category;
    }

    public String templatePath() {
        return templatePath;
    }

    public String subjectKey() {
        return subjectKey;
    }

    public MailCategory category() {
        return category;
    }
}
