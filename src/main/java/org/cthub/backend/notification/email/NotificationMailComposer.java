package org.cthub.backend.notification.email;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.render.NotificationMailModel;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Turns a notification into the texts of its e-mail, in the recipient's language. The switch is
 * exhaustive on purpose: adding a {@link org.cthub.backend.notification.model.NotificationType}
 * forces a decision about its e-mail text here.
 */
@Component
@RequiredArgsConstructor
public class NotificationMailComposer {

    /** Frontend route the mail links to when the notification itself has no link. */
    static final String NOTIFICATIONS_PATH = "/account/notifications";

    private final MessageSource mailMessageSource;
    private final MailProperties mailProperties;

    public NotificationMailModel compose(NotificationsCreatedEvent.Item item, User recipient, Locale locale) {
        Map<String, String> params = item.params();
        String typeLabel = mailMessageSource.getMessage("mail.notification.type." + item.type().name(), null, locale);
        String actionUrl = mailProperties.getBaseUrl() + (item.link() != null ? item.link() : NOTIFICATIONS_PATH);

        return switch (item.type()) {
            case ANNOUNCEMENT -> new NotificationMailModel(
                recipient.getFirstName(), params.get("title"), params.get("body"), actionUrl, typeLabel);
            case WELCOME -> throw new IllegalStateException(
                "WELCOME notifications are not delivered by e-mail; the transactional welcome mail covers them.");
        };
    }
}
