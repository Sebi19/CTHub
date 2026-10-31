package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.auth.SecureTokens;
import org.cthub.backend.model.User;
import org.cthub.backend.model.UserToken;
import org.cthub.backend.model.UserTokenType;
import org.cthub.backend.repository.UserTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Generates and validates single-use mailed tokens. The raw token is only ever handed to the caller
 * once (to embed in a link); only its SHA-256 hash is persisted.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final Duration EMAIL_CHANGE_TTL = Duration.ofHours(1);

    private final UserTokenRepository userTokenRepository;

    /**
     * Issues a new token of the given type for the user, invalidating any previous unconsumed
     * tokens of the same type, so at most one is usable at a time. Returns the raw token - embed it
     * in a link, never log it.
     */
    @Transactional
    public String issue(User user, UserTokenType type, Duration ttl, String payload) {
        List<UserToken> previous = userTokenRepository.findByUserAndTypeAndConsumedAtIsNull(user, type);
        Instant now = Instant.now();
        previous.forEach(t -> t.setConsumedAt(now));
        userTokenRepository.saveAll(previous);

        String rawToken = SecureTokens.generate();
        userTokenRepository.save(UserToken.builder()
            .user(user)
            .type(type)
            .tokenHash(SecureTokens.hash(rawToken))
            .expiresAt(now.plus(ttl))
            .payload(payload)
            .build());
        return rawToken;
    }

    /** Looks up a usable (not expired, not consumed) token by its raw value and type, without consuming it. */
    @Transactional(readOnly = true)
    public Optional<UserToken> findUsable(String rawToken, UserTokenType type) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return userTokenRepository.findByTokenHash(SecureTokens.hash(rawToken))
            .filter(t -> t.getType() == type)
            .filter(UserToken::isUsable);
    }

    /** Marks a token as consumed so it can never be used again. */
    @Transactional
    public void consume(UserToken token) {
        token.setConsumedAt(Instant.now());
        userTokenRepository.save(token);
    }
}
