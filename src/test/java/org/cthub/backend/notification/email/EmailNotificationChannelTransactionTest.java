package org.cthub.backend.notification.email;

import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.render.NotificationMailModel;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.NotificationPreferenceService;
import org.cthub.backend.notification.NotificationRequest;
import org.cthub.backend.notification.NotificationService;
import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The e-mail channel listens before commit, which never happens inside the usual rolled-back test
 * transaction. These tests therefore commit for real (and clean up afterwards) to prove that the
 * mail is queued inside the notification's own transaction - and not at all on rollback.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({NotificationService.class, NotificationPreferenceService.class, EmailNotificationChannel.class})
class EmailNotificationChannelTransactionTest {

    @TestConfiguration
    static class NoCaching {
        @Bean
        CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @MockitoBean
    private MailService mailService;
    @MockitoBean
    private NotificationMailComposer composer;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationPreferenceService preferenceService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private User subscriber;
    private User other;

    @BeforeEach
    void setUp() {
        subscriber = userRepository.save(user("subscriber"));
        other = userRepository.save(user("other"));
        preferenceService.update(subscriber.getId(), NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, true);
        when(composer.compose(any(), any(), any()))
            .thenReturn(new NotificationMailModel("S", "Title", "Body", "https://x", "Label"));
    }

    @AfterEach
    void tearDown() {
        // Notifications and preferences go with their users (ON DELETE CASCADE).
        userRepository.deleteAllById(List.of(subscriber.getId(), other.getId()));
    }

    @Test
    void queuesTheMailInsideTheNotificationTransaction() {
        AtomicBoolean insideTransaction = new AtomicBoolean();
        doAnswer(invocation -> {
            insideTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(mailService).enqueue(any(), any(), any(), any(), anyString());

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            notificationService.notify(announcement(), List.of(subscriber.getId(), other.getId())));

        verify(mailService).enqueue(
            eq(MailTemplate.NOTIFICATION), argThat(to -> to.email().equals(subscriber.getEmail())),
            any(), any(), startsWith("notification:"));
        verifyNoMoreInteractions(mailService);
        assertThat(insideTransaction).isTrue();
    }

    @Test
    void queuesNothingWhenTheTransactionRollsBack() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            notificationService.notify(announcement(), subscriber.getId());
            status.setRollbackOnly();
        });

        verifyNoInteractions(mailService);
    }

    private static NotificationRequest announcement() {
        return NotificationRequest.builder()
            .type(NotificationType.ANNOUNCEMENT)
            .params(Map.of("title", "Title", "body", "Body"))
            .build();
    }

    private static User user(String name) {
        return User.builder()
            .email(name + "-" + UUID.randomUUID() + "@notification-test.invalid")
            .role(User.Role.USER)
            .enabled(true)
            .build();
    }
}
