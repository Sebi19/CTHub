package org.cthub.backend.auth.credentials;

import org.apache.commons.codec.binary.Base32;
import org.cthub.backend.auth.AuthProperties;
import org.cthub.backend.auth.SecureTokens;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.model.User;
import org.cthub.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;

/**
 * Authenticator-app codes (RFC 6238: HMAC-SHA1, 6 digits, 30-second steps, one step of clock skew
 * either way). Only ever a second factor after the password or a step-up method - never a login on
 * its own, since six digits are far too little entropy for that.
 *
 * <p>Secrets are AES-256-GCM encrypted at rest with {@code app.security.totp-encryption-key}. Each
 * accepted step is remembered, so a code cannot be replayed within its validity window.
 */
@Service
public class TotpService {

    public static final String ISSUER = "Challenge Team Hub";
    static final int DIGITS = 6;
    static final long STEP_SECONDS = 30;
    static final int ALLOWED_DRIFT = 1;
    private static final int SECRET_BYTES = 20;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final UserRepository userRepository;
    private final Clock clock;
    private final SecretKeySpec encryptionKey;

    public TotpService(UserRepository userRepository, Clock clock, AuthProperties properties) {
        this.userRepository = userRepository;
        this.clock = clock;
        this.encryptionKey = parseKey(properties.getTotpEncryptionKey());
    }

    public record Enrollment(String secret, String otpauthUri) {
    }

    public boolean isAvailable() {
        return encryptionKey != null;
    }

    /** Starts (or restarts) enrolment; the secret only becomes active once {@link #confirm} succeeds. */
    @Transactional
    public Enrollment begin(User user) {
        requireAvailable();
        User managed = userRepository.findById(user.getId()).orElseThrow();
        byte[] secret = SecureTokens.randomBytes(SECRET_BYTES);
        managed.setTotpPendingSecretEnc(encrypt(secret));
        String base32 = new Base32().encodeToString(secret).replace("=", "");
        String label = encode(ISSUER + ":" + managed.getEmail());
        String uri = "otpauth://totp/" + label + "?secret=" + base32 + "&issuer=" + encode(ISSUER)
            + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
        return new Enrollment(base32, uri);
    }

    /** Activates the pending secret if {@code code} matches it. */
    @Transactional
    public boolean confirm(User user, String code) {
        requireAvailable();
        User managed = userRepository.findById(user.getId()).orElseThrow();
        if (managed.getTotpPendingSecretEnc() == null) {
            throw ApiException.badRequest("TOTP_NOT_PENDING", "Start the authenticator setup first.");
        }
        Long step = matchingStep(decrypt(managed.getTotpPendingSecretEnc()), code);
        if (step == null) {
            return false;
        }
        managed.setTotpSecretEnc(managed.getTotpPendingSecretEnc());
        managed.setTotpPendingSecretEnc(null);
        managed.setTotpEnabledAt(clock.instant());
        managed.setTotpLastUsedStep(step);
        return true;
    }

    /** Checks a code for an account with TOTP enabled; each time step is accepted at most once. */
    @Transactional
    public boolean verify(User user, String code) {
        if (!isAvailable()) {
            return false;
        }
        User managed = userRepository.findById(user.getId()).orElseThrow();
        if (!managed.isTotpEnabled()) {
            return false;
        }
        Long step = matchingStep(decrypt(managed.getTotpSecretEnc()), code);
        // Atomic compare-and-set, so two parallel requests cannot both spend the same code.
        return step != null && userRepository.acceptTotpStep(managed.getId(), step) == 1;
    }

    @Transactional
    public void disable(User user) {
        User managed = userRepository.findById(user.getId()).orElseThrow();
        managed.setTotpSecretEnc(null);
        managed.setTotpPendingSecretEnc(null);
        managed.setTotpEnabledAt(null);
        managed.setTotpLastUsedStep(null);
    }

    private Long matchingStep(byte[] secret, String code) {
        if (code == null) {
            return null;
        }
        String digits = code.replaceAll("\\s", "");
        if (!digits.matches("\\d{" + DIGITS + "}")) {
            return null;
        }
        long current = clock.instant().getEpochSecond() / STEP_SECONDS;
        Long matched = null;
        // No early exit, so timing does not reveal which step matched.
        for (long step = current - ALLOWED_DRIFT; step <= current + ALLOWED_DRIFT; step++) {
            boolean equal = MessageDigest.isEqual(
                generateCode(secret, step, DIGITS).getBytes(StandardCharsets.US_ASCII),
                digits.getBytes(StandardCharsets.US_ASCII));
            if (equal && matched == null) {
                matched = step;
            }
        }
        return matched;
    }

    /** RFC 4226 HOTP with HMAC-SHA1 for the given time step. */
    static String generateCode(byte[] secret, long step, int digits) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, digits);
            return String.format("%0" + digits + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 not available", e);
        }
    }

    private String encrypt(byte[] plaintext) {
        try {
            byte[] iv = SecureTokens.randomBytes(GCM_IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] sealed = cipher.doFinal(plaintext);
            return Base64.getEncoder().encodeToString(
                ByteBuffer.allocate(iv.length + sealed.length).put(iv).put(sealed).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt TOTP secret", e);
        }
    }

    private byte[] decrypt(String stored) {
        requireAvailable();
        try {
            byte[] raw = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, raw, 0, GCM_IV_BYTES));
            return cipher.doFinal(raw, GCM_IV_BYTES, raw.length - GCM_IV_BYTES);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not decrypt TOTP secret (wrong app.security.totp-encryption-key?)", e);
        }
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TOTP_UNAVAILABLE",
                "Authenticator apps are not configured on this server.");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static SecretKeySpec parseKey(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.security.totp-encryption-key is not valid base64.", e);
        }
        if (key.length != 32) {
            throw new IllegalStateException("app.security.totp-encryption-key must be 32 bytes (base64-encoded).");
        }
        return new SecretKeySpec(key, "AES");
    }
}
