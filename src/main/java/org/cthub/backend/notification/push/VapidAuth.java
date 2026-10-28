package org.cthub.backend.notification.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application server identification (VAPID, RFC 8292): signs the ES256 JWT that every push request
 * carries. Tokens are cached per push service origin and reused until they get close to expiry.
 */
@Component
@Slf4j
public class VapidAuth {

    /** RFC 8292 caps the lifetime at 24 hours; stay well below for clock skew. */
    static final Duration TOKEN_LIFETIME = Duration.ofHours(12);
    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofHours(1);
    private static final String ALGORITHM = "SHA256withECDSAinP1363Format";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Clock clock;
    private final String subject;
    private final ECPrivateKey privateKey;
    private final ECPublicKey publicKey;
    private final String publicKeyBase64;
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();

    private record Token(String header, Instant expiresAt) {
    }

    @Autowired
    public VapidAuth(PushProperties properties) {
        this(properties, Clock.systemUTC());
    }

    VapidAuth(PushProperties properties, Clock clock) {
        this.clock = clock;
        this.subject = properties.getVapid().getSubject();
        if (!properties.isEnabled()) {
            log.info("Web Push is disabled: app.push.vapid.public-key / private-key are not configured.");
            this.privateKey = null;
            this.publicKey = null;
            this.publicKeyBase64 = null;
            return;
        }
        try {
            this.publicKey = EcKeys.publicKey(EcKeys.decode(properties.getVapid().getPublicKey()));
            this.privateKey = EcKeys.privateKey(EcKeys.decode(properties.getVapid().getPrivateKey()));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.push.vapid keys are not valid base64url P-256 keys", e);
        }
        this.publicKeyBase64 = EcKeys.encode(EcKeys.encode(publicKey));
        if (subject == null || !(subject.startsWith("mailto:") || subject.startsWith("https://"))) {
            throw new IllegalStateException("app.push.vapid.subject must be a mailto: or https: URI");
        }
        verifyKeyPair();
    }

    public boolean isEnabled() {
        return privateKey != null;
    }

    /** The key browsers subscribe with ({@code applicationServerKey}). */
    public String publicKey() {
        return publicKeyBase64;
    }

    /** Value of the {@code Authorization} header for a request to {@code endpoint}. */
    public String authorization(URI endpoint) {
        if (!isEnabled()) {
            throw new IllegalStateException("Web Push is disabled");
        }
        String audience = endpoint.getScheme() + "://" + endpoint.getRawAuthority();
        Instant now = clock.instant();
        return tokens.compute(audience, (aud, cached) ->
            cached != null && cached.expiresAt().minus(RENEW_BEFORE_EXPIRY).isAfter(now) ? cached : sign(aud, now)
        ).header();
    }

    private Token sign(String audience, Instant now) {
        Instant expiresAt = now.plus(TOKEN_LIFETIME);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("aud", audience);
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("sub", subject);

        String signingInput = base64Json(Map.of("typ", "JWT", "alg", "ES256")) + "." + base64Json(claims);
        String jwt = signingInput + "." + EcKeys.encode(signRaw(signingInput.getBytes(StandardCharsets.US_ASCII)));
        return new Token("vapid t=" + jwt + ", k=" + publicKeyBase64, expiresAt);
    }

    private byte[] signRaw(byte[] data) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("VAPID signing failed", e);
        }
    }

    /** Fails startup if the configured public key does not belong to the private key. */
    private void verifyKeyPair() {
        byte[] probe = "cthub-vapid-key-check".getBytes(StandardCharsets.US_ASCII);
        try {
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(probe);
            if (!verifier.verify(signRaw(probe))) {
                throw new IllegalStateException("app.push.vapid.public-key does not match app.push.vapid.private-key");
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not verify the VAPID key pair", e);
        }
    }

    private static String base64Json(Object value) {
        return EcKeys.encode(JSON.writeValueAsBytes(value));
    }
}
