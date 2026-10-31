package org.cthub.backend.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.auth.attempt.LoginAttemptRepository;
import org.cthub.backend.auth.session.AuthSessionRepository;
import org.cthub.backend.repository.UserTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Daily housekeeping for auth data: ended sessions (their refresh tokens and push subscriptions
 * cascade), finished login attempts and used/expired mailed tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthCleanupJob {

    /** Ended sessions are kept a little while so "you were signed out" diagnostics stay possible. */
    static final Duration SESSION_RETENTION = Duration.ofDays(7);
    static final Duration ATTEMPT_RETENTION = Duration.ofDays(1);

    private final AuthSessionRepository sessionRepository;
    private final LoginAttemptRepository attemptRepository;
    private final UserTokenRepository userTokenRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanup() {
        Instant now = clock.instant();
        int attempts = attemptRepository.deleteExpiredBefore(now.minus(ATTEMPT_RETENTION));
        int sessions = sessionRepository.deleteEndedBefore(now.minus(SESSION_RETENTION));
        int tokens = userTokenRepository.deleteExpiredOrConsumed(now);
        if (attempts + sessions + tokens > 0) {
            log.info("Auth cleanup removed {} session(s), {} login attempt(s), {} token(s).", sessions, attempts, tokens);
        }
    }
}
