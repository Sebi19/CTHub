package org.cthub.backend.notification;

import org.cthub.backend.exception.ApiException;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.NotificationPreferenceService.EffectivePreference;
import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the local Postgres so V14 and the projection query are exercised for real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(NotificationPreferenceService.class)
class NotificationPreferenceServiceTest {

    @TestConfiguration
    static class NoCaching {
        @Bean
        CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @Autowired
    private NotificationPreferenceService service;
    @Autowired
    private NotificationPreferenceRepository repository;
    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(user("alice"));
        bob = userRepository.save(user("bob"));
        carol = userRepository.save(user("carol"));
    }

    @Test
    void listsOnlyConfigurableCombinationsWithOptInDefaults() {
        assertThat(service.list(alice.getId())).containsExactly(
            new EffectivePreference(NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, false),
            new EffectivePreference(NotificationType.ANNOUNCEMENT, NotificationChannel.PUSH, true));
    }

    @Test
    void updateUpsertsASingleRow() {
        service.update(alice.getId(), NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, true);
        service.update(alice.getId(), NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, false);
        service.update(alice.getId(), NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, true);

        assertThat(repository.findByUserId(alice.getId())).hasSize(1);
        assertThat(service.list(alice.getId())).containsExactly(
            new EffectivePreference(NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, true),
            new EffectivePreference(NotificationType.ANNOUNCEMENT, NotificationChannel.PUSH, true));
        assertThat(service.list(bob.getId())).containsExactly(
            new EffectivePreference(NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, false),
            new EffectivePreference(NotificationType.ANNOUNCEMENT, NotificationChannel.PUSH, true));
    }

    @Test
    void rejectsChannelsTheTypeDoesNotSupport() {
        assertThatThrownBy(() -> service.update(alice.getId(), NotificationType.WELCOME, NotificationChannel.EMAIL, true))
            .isInstanceOf(ApiException.class);
        assertThat(repository.findByUserId(alice.getId())).isEmpty();
    }

    @Test
    void recipientsAreOnlyThoseWhoOptedIn() {
        service.update(alice.getId(), NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, true);
        service.update(bob.getId(), NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, false);

        List<Long> everyone = List.of(alice.getId(), bob.getId(), carol.getId());
        assertThat(service.recipientsFor(NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, everyone))
            .containsExactly(alice.getId());
        assertThat(service.recipientsFor(NotificationType.WELCOME, NotificationChannel.EMAIL, everyone)).isEmpty();
        assertThat(service.recipientsFor(NotificationType.ANNOUNCEMENT, NotificationChannel.EMAIL, List.of())).isEmpty();
    }

    @Test
    void pushReachesEveryoneExceptExplicitOptOuts() {
        service.update(bob.getId(), NotificationType.ANNOUNCEMENT, NotificationChannel.PUSH, false);
        service.update(carol.getId(), NotificationType.ANNOUNCEMENT, NotificationChannel.PUSH, true);

        List<Long> everyone = List.of(alice.getId(), bob.getId(), carol.getId());
        assertThat(service.recipientsFor(NotificationType.ANNOUNCEMENT, NotificationChannel.PUSH, everyone))
            .containsExactlyInAnyOrder(alice.getId(), carol.getId());
        assertThat(service.recipientsFor(NotificationType.WELCOME, NotificationChannel.PUSH, everyone)).isEmpty();
    }

    private static User user(String name) {
        return User.builder()
            .email(name + "-" + UUID.randomUUID() + "@notification-test.invalid")
            .role(User.Role.USER)
            .build();
    }
}
