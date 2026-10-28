package org.cthub.backend.notification;

import jakarta.persistence.EntityManager;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.model.Notification;
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
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real JPQL against the local Postgres (Flyway-migrated, Hibernate-validated) so the
 * queries and V13's constraints are exercised for real. Every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
class NotificationRepositoryTest {

    /** The application class enables caching, which the JPA slice doesn't configure. */
    @TestConfiguration
    static class NoCaching {
        @Bean
        CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @Autowired
    private NotificationRepository repository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(user("alice"));
        bob = userRepository.save(user("bob"));
    }

    private static User user(String name) {
        return User.builder()
            .email(name + "-" + UUID.randomUUID() + "@notification-test.invalid")
            .role(User.Role.USER)
            .build();
    }

    private Notification save(User user, Instant readAt) {
        return repository.save(Notification.builder()
            .user(user)
            .type(NotificationType.ANNOUNCEMENT)
            .readAt(readAt)
            .build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void pagesNewestFirstScopedToUser() {
        Notification first = save(alice, null);
        Notification second = save(alice, Instant.now());
        Notification third = save(alice, null);
        save(bob, null);
        flushAndClear();

        List<Notification> page1 = repository.findPage(alice.getId(), Long.MAX_VALUE, false, Limit.of(2));
        List<Notification> page2 = repository.findPage(alice.getId(), page1.getLast().getId(), false, Limit.of(2));
        List<Notification> unread = repository.findPage(alice.getId(), Long.MAX_VALUE, true, Limit.of(10));

        assertThat(page1).extracting(Notification::getId).containsExactly(third.getId(), second.getId());
        assertThat(page2).extracting(Notification::getId).containsExactly(first.getId());
        assertThat(unread).extracting(Notification::getId).containsExactly(third.getId(), first.getId());
    }

    @Test
    void replaysNotificationsAfterIdOldestFirst() {
        Notification first = save(alice, null);
        Notification second = save(alice, null);
        Notification third = save(alice, null);
        save(bob, null);
        flushAndClear();

        assertThat(repository.findByUserIdAndIdGreaterThanOrderByIdAsc(alice.getId(), first.getId(), Limit.of(10)))
            .extracting(Notification::getId)
            .containsExactly(second.getId(), third.getId());
    }

    @Test
    void findsOnlyOwnNotificationById() {
        Notification own = save(alice, null);
        flushAndClear();

        assertThat(repository.findByIdAndUserId(own.getId(), alice.getId())).isPresent();
        assertThat(repository.findByIdAndUserId(own.getId(), bob.getId())).isEmpty();
    }

    @Test
    void markAllReadRespectsUpperBoundAndUser() {
        Notification older = save(alice, null);
        Notification upTo = save(alice, null);
        Notification newer = save(alice, null);
        Notification bobs = save(bob, null);
        flushAndClear();

        int updated = repository.markAllReadUpTo(alice.getId(), upTo.getId(), Instant.now());

        assertThat(updated).isEqualTo(2);
        assertThat(repository.findById(older.getId())).get().matches(Notification::isRead);
        assertThat(repository.findById(upTo.getId())).get().matches(Notification::isRead);
        assertThat(repository.findById(newer.getId())).get().matches(n -> !n.isRead());
        assertThat(repository.findById(bobs.getId())).get().matches(n -> !n.isRead());
        assertThat(repository.countByUserIdAndReadAtIsNull(alice.getId())).isEqualTo(1);
    }

    @Test
    void dedupeKeyIsUniquePerUserOnly() {
        repository.save(Notification.builder().user(alice).type(NotificationType.WELCOME).dedupeKey("welcome").build());
        repository.save(Notification.builder().user(bob).type(NotificationType.WELCOME).dedupeKey("welcome").build());
        flushAndClear();

        assertThat(repository.findUserIdsWithDedupeKey("welcome", List.of(alice.getId(), bob.getId())))
            .containsExactlyInAnyOrder(alice.getId(), bob.getId());

        repository.save(Notification.builder().user(alice).type(NotificationType.WELCOME).dedupeKey("welcome").build());
        assertThatThrownBy(entityManager::flush).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }

    @Test
    void deletesOnlyReadNotificationsOlderThanCutoff() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(90));
        Notification oldRead = save(alice, cutoff.minus(Duration.ofDays(1)));
        Notification recentRead = save(alice, Instant.now());
        Notification unread = save(alice, null);
        flushAndClear();

        repository.deleteReadBefore(cutoff);

        assertThat(repository.findById(oldRead.getId())).isEmpty();
        assertThat(repository.findById(recentRead.getId())).isPresent();
        assertThat(repository.findById(unread.getId())).isPresent();
    }

    @Test
    void persistsParamsAndLink() {
        Notification saved = repository.save(Notification.builder()
            .user(alice)
            .type(NotificationType.ANNOUNCEMENT)
            .params(Map.of("title", "Saisonstart", "body", "Ä & \"quotes\""))
            .link("/competitions")
            .build());
        flushAndClear();

        Notification loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getParams()).containsEntry("title", "Saisonstart").containsEntry("body", "Ä & \"quotes\"");
        assertThat(loaded.getLink()).isEqualTo("/competitions");
        assertThat(loaded.getCreatedAt()).isNotNull();
    }
}
