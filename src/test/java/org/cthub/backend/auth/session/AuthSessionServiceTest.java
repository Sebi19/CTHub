package org.cthub.backend.auth.session;

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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Rotation, the parallel-tab grace window and reuse detection, against the real schema. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(AuthSessionService.class)
class AuthSessionServiceTest {

    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        MutableClock clock() {
            return new MutableClock();
        }

        @Bean
        CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @Autowired
    private AuthSessionService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository tokenRepository;
    @Autowired
    private AuthSessionRepository sessionRepository;
    @Autowired
    private MutableClock clock;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
            .email("session-" + UUID.randomUUID() + "@session-test.invalid")
            .enabled(true)
            .role(User.Role.USER)
            .build());
    }

    @Test
    void rotationIssuesANewTokenAndExtendsTheSession() {
        AuthSessionService.Issued issued = service.create(user, EnumSet.of(AuthMethod.EMAIL), "UA", "127.0.0.1");
        clock.advance(Duration.ofDays(10));

        AuthSessionService.RefreshOutcome outcome = service.refresh(issued.refreshToken(), "UA", "127.0.0.1");

        assertThat(outcome).isInstanceOf(AuthSessionService.RefreshOutcome.Rotated.class);
        String next = ((AuthSessionService.RefreshOutcome.Rotated) outcome).refreshToken();
        assertThat(next).isNotEqualTo(issued.refreshToken());
        assertThat(sessionRepository.findById(issued.session().getId()).orElseThrow().getExpiresAt())
            .isEqualTo(clock.instant().plus(AuthSessionService.SESSION_LIFETIME));
        assertThat(service.refresh(next, "UA", "127.0.0.1"))
            .isInstanceOf(AuthSessionService.RefreshOutcome.Rotated.class);
    }

    @Test
    void aParallelRequestInsideTheGraceWindowIsAuthenticatedWithoutASecondSuccessor() {
        AuthSessionService.Issued issued = service.create(user, EnumSet.of(AuthMethod.EMAIL), "UA", null);
        AuthSessionService.RefreshOutcome first = service.refresh(issued.refreshToken(), "UA", null);
        clock.advance(Duration.ofSeconds(5));

        AuthSessionService.RefreshOutcome second = service.refresh(issued.refreshToken(), "UA", null);

        assertThat(first).isInstanceOf(AuthSessionService.RefreshOutcome.Rotated.class);
        assertThat(second).isEqualTo(new AuthSessionService.RefreshOutcome.Grace(issued.session().getId(), user.getId()));
        long tokens = tokenRepository.findAll().stream()
            .filter(token -> token.getSession().getId().equals(issued.session().getId()))
            .count();
        assertThat(tokens).as("original + exactly one successor").isEqualTo(2);
        assertThat(sessionRepository.findById(issued.session().getId()).orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void replayingARotatedTokenAfterTheGraceWindowRevokesTheSession() {
        AuthSessionService.Issued issued = service.create(user, EnumSet.of(AuthMethod.EMAIL), "UA", null);
        String next = ((AuthSessionService.RefreshOutcome.Rotated) service.refresh(issued.refreshToken(), "UA", null))
            .refreshToken();
        clock.advance(AuthSessionService.ROTATION_GRACE.plusSeconds(1));

        assertThat(service.refresh(issued.refreshToken(), "UA", null))
            .isInstanceOf(AuthSessionService.RefreshOutcome.Invalid.class);

        AuthSession session = sessionRepository.findById(issued.session().getId()).orElseThrow();
        assertThat(session.getRevokeReason()).isEqualTo(SessionRevokeReason.REUSE_DETECTED);
        assertThat(service.refresh(next, "UA", null)).isInstanceOf(AuthSessionService.RefreshOutcome.Invalid.class);
    }

    @Test
    void replayingAnOlderGenerationEvenWithinTheWindowRevokesTheSession() {
        AuthSessionService.Issued issued = service.create(user, EnumSet.of(AuthMethod.EMAIL), "UA", null);
        String second = ((AuthSessionService.RefreshOutcome.Rotated) service.refresh(issued.refreshToken(), "UA", null))
            .refreshToken();
        service.refresh(second, "UA", null);

        // The first token's successor has itself been rotated, so this is a genuine replay.
        assertThat(service.refresh(issued.refreshToken(), "UA", null))
            .isInstanceOf(AuthSessionService.RefreshOutcome.Invalid.class);
        assertThat(sessionRepository.findById(issued.session().getId()).orElseThrow().getRevokeReason())
            .isEqualTo(SessionRevokeReason.REUSE_DETECTED);
    }

    @Test
    void expiredAndUnknownTokensAreInvalid() {
        AuthSessionService.Issued issued = service.create(user, EnumSet.of(AuthMethod.PASSKEY), "UA", null);
        clock.advance(AuthSessionService.SESSION_LIFETIME.plusMinutes(1));

        assertThat(service.refresh(issued.refreshToken(), "UA", null))
            .isInstanceOf(AuthSessionService.RefreshOutcome.Invalid.class);
        assertThat(service.refresh("unknown", "UA", null)).isInstanceOf(AuthSessionService.RefreshOutcome.Invalid.class);
        assertThat(service.refresh(null, "UA", null)).isInstanceOf(AuthSessionService.RefreshOutcome.Invalid.class);
    }

    @Test
    void aNewSessionCountsAsSteppedUp() {
        AuthSessionService.Issued issued = service.create(user, EnumSet.of(AuthMethod.EMAIL), "UA", null);

        assertThat(service.stepUpAt(issued.session().getId())).contains(clock.instant());
    }
}
