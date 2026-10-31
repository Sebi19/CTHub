package org.cthub.backend.mail.model;

/**
 * Registry of all mail templates owned by this repo. {@code templatePath} is resolved relative to
 * {@code mail/templates/} (without extension; the renderer appends {@code .html}/{@code .txt}).
 * {@code subjectKey} is looked up in the {@code mail_<locale>.properties} bundles.
 */
public enum MailTemplate {
    WELCOME("welcome", "mail.welcome.subject", MailCategory.TRANSACTIONAL),
    /** The e-mailed half of a login / step-up challenge; also how new accounts come into being. */
    LOGIN_LINK("login-link", "mail.loginLink.subject", MailCategory.TRANSACTIONAL),
    /** Confirms a new address before an account's e-mail is changed to it. */
    EMAIL_CHANGE("email-change", "mail.emailChange.subject", MailCategory.TRANSACTIONAL),
    /** Tells the account owner about a security-relevant change (password, 2FA, passkeys, e-mail). */
    SECURITY_ALERT("security-alert", "mail.securityAlert.subject", MailCategory.TRANSACTIONAL),
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
