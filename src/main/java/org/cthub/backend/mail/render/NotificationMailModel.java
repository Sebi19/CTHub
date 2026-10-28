package org.cthub.backend.mail.render;

import java.util.Arrays;
import java.util.List;

/**
 * Model for the generic {@code notification} template, which delivers any in-app notification by
 * e-mail. Texts arrive already resolved in the recipient's language.
 *
 * @param title     headline, also used as the subject
 * @param body      plain text; blank lines separate paragraphs, single line breaks are kept
 * @param actionUrl absolute URL of the notification's target page
 * @param typeLabel localized name of the notification type, used in the "why am I getting this" note
 */
public record NotificationMailModel(
    String firstName,
    String title,
    String body,
    String actionUrl,
    String typeLabel
) implements MailTemplateModel {

    public NotificationMailModel {
        // The title becomes the subject header, which must stay on one line.
        title = title == null ? "" : title.replaceAll("\\s*\\R\\s*", " ").strip();
        body = body == null ? "" : body.strip();
    }

    @Override
    public Object[] subjectArgs() {
        return new Object[]{title};
    }

    /** {@link #body()} split into paragraphs, each a list of lines. */
    public List<List<String>> paragraphs() {
        if (body.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(body.split("\\R\\s*\\R"))
            .map(String::strip)
            .filter(paragraph -> !paragraph.isEmpty())
            .map(paragraph -> Arrays.stream(paragraph.split("\\R")).map(String::strip).toList())
            .toList();
    }
}
