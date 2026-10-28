package org.cthub.backend.notification.email;

import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.render.NotificationMailModel;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.NotificationPreferenceService;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.cthub.backend.notification.event.NotificationsCreatedEvent.Item;
import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock
    private NotificationPreferenceService preferenceService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationMailComposer composer;
    @Mock
    private MailService mailService;

    private EmailNotificationChannel channel;

    private final NotificationMailModel model = new NotificationMailModel("A", "Title", "Body", "https://x", "Label");

    @BeforeEach
    void setUp() {
        channel = new EmailNotificationChannel(preferenceService, userRepository, composer, mailService);
    }

    @Test
    void mailsOnlyOptedInEnabledUsersInTheirLanguage() {
        User alice = user(1L, "alice@example.org", "en", true);
        User disabled = user(3L, "disabled@example.org", null, false);
        when(preferenceService.recipientsFor(NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, List.of(1L, 2L, 3L)))
            .thenReturn(Set.of(1L, 3L));
        when(userRepository.findAllById(Set.of(1L, 3L))).thenReturn(List.of(alice, disabled));
        when(composer.compose(any(), eq(alice), eq(Locale.ENGLISH))).thenReturn(model);

        channel.onCreated(new NotificationsCreatedEvent(List.of(
            item(10L, 1L, NotificationType.ANNOUNCEMENT),
            item(11L, 2L, NotificationType.ANNOUNCEMENT),
            item(12L, 3L, NotificationType.ANNOUNCEMENT))));

        verify(mailService).enqueue(
            MailTemplate.NOTIFICATION, new MailAddress("alice@example.org", "Alice"), model, Locale.ENGLISH, "notification:10");
        verifyNoMoreInteractions(mailService);
    }

    @Test
    void ignoresTypesWithoutEmailSupport() {
        channel.onCreated(new NotificationsCreatedEvent(List.of(item(10L, 1L, NotificationType.WELCOME))));

        verifyNoInteractions(preferenceService, userRepository, composer, mailService);
    }

    @Test
    void doesNotLoadUsersWhenNobodyOptedIn() {
        when(preferenceService.recipientsFor(any(), any(), anyCollection())).thenReturn(Set.of());

        channel.onCreated(new NotificationsCreatedEvent(List.of(item(10L, 1L, NotificationType.ANNOUNCEMENT))));

        verifyNoInteractions(userRepository, mailService);
    }

    @Test
    void oneFailingMailDoesNotStopTheOthers() {
        User alice = user(1L, "alice@example.org", "de", true);
        User bob = user(2L, "bob@example.org", "de", true);
        when(preferenceService.recipientsFor(any(), any(), anyCollection())).thenReturn(Set.of(1L, 2L));
        when(userRepository.findAllById(any())).thenReturn(List.of(alice, bob));
        when(composer.compose(any(), eq(alice), any())).thenThrow(new IllegalStateException("template bug"));
        when(composer.compose(any(), eq(bob), any())).thenReturn(model);

        channel.onCreated(new NotificationsCreatedEvent(List.of(
            item(10L, 1L, NotificationType.ANNOUNCEMENT),
            item(11L, 2L, NotificationType.ANNOUNCEMENT))));

        verify(mailService).enqueue(eq(MailTemplate.NOTIFICATION), any(), eq(model), eq(Locale.GERMAN), eq("notification:11"));
        verifyNoMoreInteractions(mailService);
    }

    private static Item item(long id, long userId, NotificationType type) {
        return new Item(id, userId, type, Map.of("title", "T", "body", "B"), null, Instant.now());
    }

    private static User user(long id, String email, String locale, boolean enabled) {
        String name = email.substring(0, 1).toUpperCase() + email.substring(1, email.indexOf('@'));
        return User.builder().id(id).email(email).firstName(name).preferredLocale(locale).enabled(enabled)
            .role(User.Role.USER).build();
    }
}
