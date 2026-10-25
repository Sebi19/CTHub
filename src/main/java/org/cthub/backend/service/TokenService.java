package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.model.User;
import org.cthub.backend.model.UserToken;
import org.cthub.backend.model.UserTokenType;
import org.cthub.backend.repository.UserTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Generates and validates single-use tokens for e-mail verification and password reset. The raw
 * token is only ever handed to the caller once (to embed in a link); only its SHA-256 hash is
 * persisted, so a DB leak alone can never be used to impersonate a user.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);
    public static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);
    /** Short-lived on purpose: a sign-in link sitting in an inbox is a standing credential. */
    public static final Duration LOGIN_LINK_TTL = Duration.ofMinutes(15);
    /** Outlives the verification link so the pending page keeps working for the whole attempt. */
    public static final Duration REGISTRATION_STATUS_TTL = Duration.ofHours(25);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserTokenRepository userTokenRepository;

    /**
     * Issues a new token of the given type for the user, invalidating any previous unconsumed
     * tokens of the same type. Returns the raw (unhashed) token - embed it in a link, never log it.
     */
    @Transactional
    public String issue(User user, UserTokenType type, Duration ttl) {
        return issueToken(user, type, ttl).rawToken();
    }

    /** Raw token plus the persisted row, for callers that need to annotate the token further. */
    public record IssuedToken(String rawToken, UserToken token) {
    }

    /**
     * As {@link #issue}, but also hands back the persisted entity so the caller can attach extra
     * state to it (used by the registration challenge).
     */
    @Transactional
    public IssuedToken issueToken(User user, UserTokenType type, Duration ttl) {
        List<UserToken> previous = userTokenRepository.findByUserAndTypeAndConsumedAtIsNull(user, type);
        Instant now = Instant.now();
        previous.forEach(t -> t.setConsumedAt(now));
        userTokenRepository.saveAll(previous);

        String rawToken = generateRawToken();
        UserToken token = UserToken.builder()
            .user(user)
            .type(type)
            .tokenHash(hash(rawToken))
            .expiresAt(now.plus(ttl))
            .build();
        userTokenRepository.save(token);

        return new IssuedToken(rawToken, token);
    }

    /**
     * Finds the single usable token of a type for a user, without needing its raw value.
     *
     * <p>{@link #issue} consumes older tokens of the same type, so at most one can be usable at a
     * time. Used to resolve a registration attempt from the verification link that belongs to it.
     */
    @Transactional(readOnly = true)
    public Optional<UserToken> findUsableByUser(User user, UserTokenType type) {
        return userTokenRepository.findByUserAndTypeAndConsumedAtIsNull(user, type).stream()
            .filter(UserToken::isUsable)
            .findFirst();
    }

    /** Persists changes made to a token by a caller (challenge bookkeeping). */
    @Transactional
    public UserToken save(UserToken token) {
        return userTokenRepository.save(token);
    }

    /**
     * Looks up a usable (not expired, not consumed) token by its raw value and type, without
     * consuming it.
     */
    @Transactional(readOnly = true)
    public Optional<UserToken> findUsable(String rawToken, UserTokenType type) {
        return userTokenRepository.findByTokenHash(hash(rawToken))
            .filter(t -> t.getType() == type)
            .filter(UserToken::isUsable);
    }

    /** Marks a token as consumed so it can never be used again. */
    @Transactional
    public void consume(UserToken token) {
        token.setConsumedAt(Instant.now());
        userTokenRepository.save(token);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
