package org.cthub.backend.mail.render;

/**
 * Marker interface implemented by one record per {@link org.cthub.backend.mail.model.MailTemplate},
 * keeping the Thymeleaf model surface explicit and unit-testable (e.g.
 * {@code RegistrationConfirmationModel(String firstName, String confirmUrl, Duration validity)}).
 */
public interface MailTemplateModel {

    /** Arguments for the template's subject message; {@code null} when the subject is fixed text. */
    default Object[] subjectArgs() {
        return null;
    }
}
