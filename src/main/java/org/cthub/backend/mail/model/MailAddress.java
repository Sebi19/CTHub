package org.cthub.backend.mail.model;

/**
 * A single e-mail recipient/sender address with an optional display name.
 */
public record MailAddress(String email, String name) {

    public static MailAddress of(String email) {
        return new MailAddress(email, null);
    }

    public static MailAddress of(String email, String name) {
        return new MailAddress(email, name);
    }
}
