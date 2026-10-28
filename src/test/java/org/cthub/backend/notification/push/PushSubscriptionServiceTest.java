package org.cthub.backend.notification.push;

import org.cthub.backend.exception.ApiException;
import org.cthub.backend.model.User;
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

import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the local Postgres so V15 and the projection query are exercised for real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(PushSubscriptionService.class)
class PushSubscriptionServiceTest {

    @TestConfiguration
    static class Config {
        @Bean
        PushProperties pushProperties() {
            PushProperties properties = new PushProperties();
            properties.setMaxSubscriptionsPerUser(2);
            return properties;
        }

        @Bean
        CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @Autowired
    private PushSubscriptionService service;
    @Autowired
    private PushSubscriptionRepository repository;
    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(user("alice"));
        bob = userRepository.save(user("bob"));
    }

    @Test
    void registeringTwiceRefreshesTheSameRow() {
        String endpoint = endpoint();
        PushSubscription first = service.register(alice.getId(), registration(endpoint, null), "UA 1");
        PushSubscription second = service.register(alice.getId(), registration(endpoint, null), "UA 2");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(service.list(alice.getId())).singleElement()
            .satisfies(s -> assertThat(s.getUserAgent()).isEqualTo("UA 2"));
    }

    @Test
    void anEndpointMovesToTheUserWhoRegisteredItLast() {
        String endpoint = endpoint();
        service.register(alice.getId(), registration(endpoint, null), null);
        service.register(bob.getId(), registration(endpoint, null), null);

        assertThat(service.list(alice.getId())).isEmpty();
        assertThat(service.list(bob.getId())).hasSize(1);
        assertThat(repository.findDeliverable(java.util.List.of(alice.getId(), bob.getId())))
            .singleElement().satisfies(t -> assertThat(t.userId()).isEqualTo(bob.getId()));
    }

    @Test
    void renewOnlyRefreshesButNeverCreates() {
        String endpoint = endpoint();
        PushSubscriptionService.Registration renew = new PushSubscriptionService.Registration(
            endpoint, validKey(), EcKeys.encode(new byte[16]), true, null, true);

        assertThatThrownBy(() -> service.register(alice.getId(), renew, null)).isInstanceOf(ApiException.class);
        service.register(alice.getId(), registration(endpoint, null), null);
        assertThatThrownBy(() -> service.register(bob.getId(), renew, null)).isInstanceOf(ApiException.class);
        assertThat(service.register(alice.getId(), renew, "UA").getUserAgent()).isEqualTo("UA");
    }

    @Test
    void rotationReplacesThePreviousEndpoint() {
        String old = endpoint();
        service.register(alice.getId(), registration(old, null), null);
        String rotated = endpoint();
        service.register(alice.getId(), registration(rotated, old), null);

        assertThat(repository.findByEndpoint(old)).isEmpty();
        assertThat(repository.findByEndpoint(rotated)).isPresent();
    }

    @Test
    void dropsTheLeastRecentlySeenDeviceBeyondTheLimit() {
        PushSubscription stale = service.register(alice.getId(), registration(endpoint(), null), null);
        PushSubscription recent = service.register(alice.getId(), registration(endpoint(), null), null);
        stale.setLastSeenAt(Instant.now().minus(Duration.ofDays(30)));
        recent.setLastSeenAt(Instant.now().minus(Duration.ofDays(1)));

        PushSubscription added = service.register(alice.getId(), registration(endpoint(), null), null);

        assertThat(service.list(alice.getId())).extracting(PushSubscription::getId)
            .containsExactlyInAnyOrder(recent.getId(), added.getId());
    }

    @Test
    void rejectsEndpointsOutsideKnownPushServices() {
        for (String bad : new String[]{
            "http://fcm.googleapis.com/fcm/send/x",
            "https://evil.example.org/fcm.googleapis.com",
            "https://fcm.googleapis.com.evil.org/x",
            "https://fcm.googleapis.com:8443/x",
            "https://user@fcm.googleapis.com/x",
            "https://localhost/x",
            "not a url"}) {
            assertThatThrownBy(() -> service.register(alice.getId(), registration(bad, null), null))
                .as(bad).isInstanceOf(ApiException.class);
        }
        assertThat(service.isAllowedEndpoint("https://updates.push.services.mozilla.com/wpush/v2/x")).isTrue();
        assertThat(service.isAllowedEndpoint("https://web.push.apple.com/abc")).isTrue();
        assertThat(service.isAllowedEndpoint("https://wns2-par02p.notify.windows.com/w/?token=x")).isTrue();
        assertThat(service.list(alice.getId())).isEmpty();
    }

    @Test
    void rejectsInvalidKeys() {
        PushSubscriptionService.Registration badKey = new PushSubscriptionService.Registration(
            endpoint(), EcKeys.encode(new byte[65]), EcKeys.encode(new byte[16]), false, null, false);
        PushSubscriptionService.Registration badAuth = new PushSubscriptionService.Registration(
            endpoint(), validKey(), EcKeys.encode(new byte[8]), false, null, false);

        assertThatThrownBy(() -> service.register(alice.getId(), badKey, null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.register(alice.getId(), badAuth, null)).isInstanceOf(ApiException.class);
    }

    @Test
    void usersCanOnlyRemoveTheirOwnDevices() {
        PushSubscription subscription = service.register(alice.getId(), registration(endpoint(), null), null);

        assertThatThrownBy(() -> service.delete(bob.getId(), subscription.getId())).isInstanceOf(ApiException.class);
        service.unregister(bob.getId(), subscription.getEndpoint());
        assertThat(service.list(alice.getId())).hasSize(1);

        service.unregister(alice.getId(), subscription.getEndpoint());
        assertThat(repository.findByEndpoint(subscription.getEndpoint())).isEmpty();
    }

    private static PushSubscriptionService.Registration registration(String endpoint, String previous) {
        return new PushSubscriptionService.Registration(endpoint, validKey(), EcKeys.encode(new byte[16]), true, previous, false);
    }

    private static String validKey() {
        return EcKeys.encode(EcKeys.encode((ECPublicKey) EcKeys.generate().getPublic()));
    }

    private static String endpoint() {
        return "https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID();
    }

    private static User user(String name) {
        return User.builder()
            .email(name + "-" + UUID.randomUUID() + "@push-test.invalid")
            .enabled(true)
            .role(User.Role.USER)
            .build();
    }
}
