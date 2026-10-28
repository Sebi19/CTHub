package org.cthub.backend.notification.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.NotificationPreferenceService;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Delivers notifications by e-mail to recipients who opted in for the notification's type.
 *
 * <p>Runs <em>before</em> commit, inside the transaction that created the notifications: the mails
 * go into the transactional outbox, so a notification and its e-mail are committed (or rolled back)
 * together, and the outbox handles retries. The outbox dedupe key is the notification id, which is
 * unique and never repeats.
 *
 * <p>Mails are rendered synchronously, so a broadcast to many opted-in users takes a moment longer
 * to commit. That is fine at the current user count; move rendering behind a queue if it grows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel {

    static final String DEDUPE_PREFIX = "notification:";

    private final NotificationPreferenceService preferenceService;
    private final UserRepository userRepository;
    private final NotificationMailComposer composer;
    private final MailService mailService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onCreated(NotificationsCreatedEvent event) {
        Map<NotificationType, List<NotificationsCreatedEvent.Item>> byType = event.notifications().stream()
            .filter(item -> item.type().supports(NotificationChannel.EMAIL))
            .collect(Collectors.groupingBy(NotificationsCreatedEvent.Item::type));

        byType.forEach((type, items) -> {
            Set<Long> optedIn = preferenceService.recipientsFor(
                type, NotificationChannel.EMAIL, items.stream().map(NotificationsCreatedEvent.Item::userId).toList());
            if (optedIn.isEmpty()) {
                return;
            }
            Map<Long, User> users = userRepository.findAllById(optedIn).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

            items.stream()
                .filter(item -> optedIn.contains(item.userId()))
                .forEach(item -> send(item, users.get(item.userId())));
        });
    }

    private void send(NotificationsCreatedEvent.Item item, User user) {
        if (user == null || !user.isEnabled() || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        Locale locale = user.getPreferredLocale() != null ? Locale.forLanguageTag(user.getPreferredLocale()) : Locale.GERMAN;
        try {
            mailService.enqueue(
                MailTemplate.NOTIFICATION,
                new MailAddress(user.getEmail(), displayName(user)),
                composer.compose(item, user, locale),
                locale,
                DEDUPE_PREFIX + item.id());
        } catch (RuntimeException e) {
            // A rendering bug must not cost the user the in-app notification, which is the source of truth.
            log.error("Could not queue e-mail for notification {} ({}) to user {}.", item.id(), item.type(), user.getId(), e);
        }
    }

    private static String displayName(User user) {
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
            + (user.getLastName() != null ? user.getLastName() : "")).strip();
        return name.isEmpty() ? null : name;
    }
}
