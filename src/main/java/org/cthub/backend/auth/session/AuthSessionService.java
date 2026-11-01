package org.cthub.backend.auth.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.auth.SecureTokens;
import org.cthub.backend.model.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Long-lived login sessions and their rotating refresh tokens.
 *
 * <p>Refresh semantics:
 * <ul>
 *   <li>A live, never-rotated token is rotated: it is marked rotated and exactly one successor is
 *       inserted, atomically ({@link RefreshTokenRepository#markRotated}). The session's lifetime
 *       slides forward: 90 days for browsers, a year for the installed app, which keeps phones that
 *       only approve sign-ins signed in across a season break.</li>
 *   <li>Parallel tabs racing on the same token: the loser presents a token rotated within the last
 *       {@link #ROTATION_GRACE}. If that token's direct successor is still the live head, the request
 *       is authenticated from the session only ({@link RefreshOutcome.Grace}) - no new token, no
 *       cookie, no second branch. The winner's response already carries the new cookie.</li>
 *   <li>Any other already-rotated token is a replay: the whole session is revoked.</li>
 * </ul>
 */
@Service
@Slf4j
public class AuthSessionService {

    public static final Duration SESSION_LIFETIME = Duration.ofDays(90);
    public static final Duration INSTALLED_APP_SESSION_LIFETIME = Duration.ofDays(365);
    public static final Duration ROTATION_GRACE = Duration.ofSeconds(30);
    private static final Duration LIVE_CACHE_TTL = Duration.ofSeconds(30);
    private static final int MAX_USER_AGENT = 512;
    private static final int MAX_IP = 64;

    private final AuthSessionRepository sessionRepository;
    private final RefreshTokenRepository tokenRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Cache<UUID, Boolean> liveCache = Caffeine.newBuilder()
        .expireAfterWrite(LIVE_CACHE_TTL)
        .maximumSize(50_000)
        .build();

    public AuthSessionService(AuthSessionRepository sessionRepository, RefreshTokenRepository tokenRepository,
                              ApplicationEventPublisher events, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.tokenRepository = tokenRepository;
        this.events = events;
        this.clock = clock;
    }

    public record Issued(AuthSession session, String refreshToken) {
    }

    public sealed interface RefreshOutcome {
        /** The token was rotated; {@code refreshToken} replaces the cookie. */
        record Rotated(UUID sessionId, long userId, String refreshToken) implements RefreshOutcome {
        }

        /** A parallel request already rotated this token moments ago; authenticate this request only. */
        record Grace(UUID sessionId, long userId) implements RefreshOutcome {
        }

        record Invalid() implements RefreshOutcome {
        }
    }

    /** Starts a session; counts as a fresh identity proof for step-up purposes. */
    @Transactional
    public Issued create(User user, Set<AuthMethod> methods, String userAgent, String ip) {
        Instant now = clock.instant();
        AuthSession session = sessionRepository.save(AuthSession.builder()
            .id(UUID.randomUUID())
            .user(user)
            .createdAt(now)
            .lastUsedAt(now)
            .expiresAt(now.plus(SESSION_LIFETIME))
            .userAgent(truncate(userAgent, MAX_USER_AGENT))
            .ipAddress(truncate(ip, MAX_IP))
            .authMethods(AuthSession.encodeMethods(methods))
            .stepUpAt(now)
            .build());
        String raw = SecureTokens.generate();
        tokenRepository.save(RefreshToken.builder()
            .session(session)
            .tokenHash(SecureTokens.hash(raw))
            .createdAt(now)
            .build());
        return new Issued(session, raw);
    }

    @Transactional
    public RefreshOutcome refresh(String rawToken, String userAgent, String ip) {
        if (rawToken == null || rawToken.isBlank()) {
            return new RefreshOutcome.Invalid();
        }
        Optional<RefreshToken> found = tokenRepository.findByTokenHash(SecureTokens.hash(rawToken));
        if (found.isEmpty()) {
            return new RefreshOutcome.Invalid();
        }
        RefreshToken token = found.get();
        long tokenId = token.getId();
        AuthSession session = token.getSession();
        UUID sessionId = session.getId();
        long userId = session.getUser().getId();
        Instant now = clock.instant();

        if (!session.isLive(now)) {
            return new RefreshOutcome.Invalid();
        }

        if (token.getRotatedAt() == null && tokenRepository.markRotated(tokenId, now) == 1) {
            AuthSession fresh = sessionRepository.findById(sessionId).orElseThrow();
            String raw = SecureTokens.generate();
            tokenRepository.save(RefreshToken.builder()
                .session(fresh)
                .parentId(tokenId)
                .tokenHash(SecureTokens.hash(raw))
                .createdAt(now)
                .build());
            fresh.setLastUsedAt(now);
            fresh.setExpiresAt(now.plus(lifetimeOf(fresh)));
            fresh.setUserAgent(truncate(userAgent, MAX_USER_AGENT));
            fresh.setIpAddress(truncate(ip, MAX_IP));
            return new RefreshOutcome.Rotated(sessionId, userId, raw);
        }

        // Already rotated - either earlier, or by a request that won the race just now (markRotated
        // blocked until it committed, so its successor is visible to the reads below).
        RefreshToken current = tokenRepository.findById(tokenId).orElseThrow();
        boolean withinGrace = current.getRotatedAt() != null
            && !current.getRotatedAt().isBefore(now.minus(ROTATION_GRACE));
        if (withinGrace) {
            boolean successorIsHead = tokenRepository.findByParentId(tokenId)
                .map(child -> child.getRotatedAt() == null)
                .orElse(false);
            if (successorIsHead) {
                return new RefreshOutcome.Grace(sessionId, userId);
            }
        }

        log.warn("Refresh token reuse detected on session {} of user {}; revoking the session.", sessionId, userId);
        revoke(sessionId, SessionRevokeReason.REUSE_DETECTED);
        return new RefreshOutcome.Invalid();
    }

    /** Whether the session may still be used. Cached briefly; revocations on this instance evict it. */
    public boolean isLive(UUID sessionId) {
        return liveCache.get(sessionId, id -> sessionRepository.findById(id)
            .map(session -> session.isLive(clock.instant()))
            .orElse(false));
    }

    @Transactional(readOnly = true)
    public Optional<AuthSession> findLive(UUID sessionId) {
        return sessionRepository.findById(sessionId).filter(session -> session.isLive(clock.instant()));
    }

    /** Revokes the session the given refresh token belongs to, if any. Used when a browser signs in again. */
    @Transactional
    public void revokeByRefreshToken(String rawToken, SessionRevokeReason reason) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        tokenRepository.findByTokenHash(SecureTokens.hash(rawToken))
            .ifPresent(token -> revoke(token.getSession().getId(), reason));
    }

    @Transactional
    public void revoke(UUID sessionId, SessionRevokeReason reason) {
        sessionRepository.findById(sessionId)
            .filter(session -> session.getRevokedAt() == null)
            .ifPresent(session -> revoke(session, reason));
    }

    /** Revokes one of the user's own sessions; {@code false} if it is not theirs or already ended. */
    @Transactional
    public boolean revokeOwn(long userId, UUID sessionId) {
        return sessionRepository.findById(sessionId)
            .filter(session -> session.getUser().getId() == userId && session.isLive(clock.instant()))
            .map(session -> {
                revoke(session, SessionRevokeReason.USER_REVOKED);
                return true;
            })
            .orElse(false);
    }

    /** Ends every live session of the user except {@code keep} (may be {@code null}). */
    @Transactional
    public int revokeAllExcept(long userId, UUID keep, SessionRevokeReason reason) {
        List<AuthSession> live = sessionRepository.findLiveByUserId(userId, clock.instant());
        int count = 0;
        for (AuthSession session : live) {
            if (!session.getId().equals(keep)) {
                revoke(session, reason);
                count++;
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<AuthSession> listLive(long userId) {
        return sessionRepository.findLiveByUserId(userId, clock.instant());
    }

    /** Records a fresh identity proof on the session (for {@code @RequiresStepUp}). */
    @Transactional
    public void markSteppedUp(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> session.setStepUpAt(clock.instant()));
    }

    /**
     * Flags a live session as the installed app and extends it to the longer lifetime right away.
     * Self-reported by the client; it only stretches the user's own session, which stays rotated,
     * reuse-protected and revocable from the session list.
     */
    @Transactional
    public void markInstalledApp(UUID sessionId) {
        Instant now = clock.instant();
        sessionRepository.findById(sessionId)
            .filter(session -> session.isLive(now) && !session.isInstalledApp())
            .ifPresent(session -> {
                session.setInstalledApp(true);
                session.setExpiresAt(now.plus(INSTALLED_APP_SESSION_LIFETIME));
            });
    }

    public static Duration lifetimeOf(AuthSession session) {
        return session.isInstalledApp() ? INSTALLED_APP_SESSION_LIFETIME : SESSION_LIFETIME;
    }

    @Transactional(readOnly = true)
    public Optional<Instant> stepUpAt(UUID sessionId) {
        return findLive(sessionId).map(AuthSession::getStepUpAt);
    }

    private void revoke(AuthSession session, SessionRevokeReason reason) {
        session.setRevokedAt(clock.instant());
        session.setRevokeReason(reason);
        sessionRepository.save(session);
        UUID id = session.getId();
        events.publishEvent(new AuthSessionRevokedEvent(id, session.getUser().getId(), reason));
        liveCache.put(id, false);
        afterCompletion(() -> liveCache.invalidate(id));
    }

    /** Re-reads after the transaction so a rollback cannot leave a live session cached as revoked. */
    private static void afterCompletion(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        action.run();
                    }
                }
            });
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
