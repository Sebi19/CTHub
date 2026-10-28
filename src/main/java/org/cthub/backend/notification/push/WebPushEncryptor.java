package org.cthub.backend.notification.push;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

/**
 * Message encryption for Web Push (RFC 8291) using the {@code aes128gcm} content coding
 * (RFC 8188) with a single record. Verified against the RFC 8291 Appendix A test vector.
 */
public final class WebPushEncryptor {

    static final int RECORD_SIZE = 4096;
    private static final int SALT_LENGTH = 16;
    private static final int AUTH_SECRET_LENGTH = 16;
    private static final int TAG_LENGTH = 16;
    private static final int HEADER_LENGTH = SALT_LENGTH + 4 + 1 + EcKeys.PUBLIC_KEY_LENGTH;

    /**
     * Push services must accept 4096 bytes of body (RFC 8030); minus header, GCM tag and the padding
     * delimiter, that leaves this much plaintext.
     */
    public static final int MAX_PLAINTEXT = RECORD_SIZE - HEADER_LENGTH - TAG_LENGTH - 1;

    private static final byte[] KEY_INFO_PREFIX = "WebPush: info\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CEK_INFO = "Content-Encoding: aes128gcm\0\1".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NONCE_INFO = "Content-Encoding: nonce\0\1".getBytes(StandardCharsets.US_ASCII);

    private static final SecureRandom RANDOM = new SecureRandom();

    private WebPushEncryptor() {
    }

    /**
     * @param uaPublic   the subscription's {@code p256dh} key (65-byte uncompressed point)
     * @param authSecret the subscription's 16-byte {@code auth} secret
     * @return the request body to POST to the push endpoint
     */
    public static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return encrypt(plaintext, uaPublic, authSecret, EcKeys.generate(), salt);
    }

    /** Deterministic variant: the ephemeral key pair and salt must be fresh for every message. */
    static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret, KeyPair ephemeral, byte[] salt) {
        if (plaintext.length > MAX_PLAINTEXT) {
            throw new IllegalArgumentException("Push payload too large: " + plaintext.length + " > " + MAX_PLAINTEXT);
        }
        if (authSecret == null || authSecret.length != AUTH_SECRET_LENGTH) {
            throw new IllegalArgumentException("The auth secret must be 16 bytes");
        }
        ECPublicKey uaKey = EcKeys.publicKey(uaPublic);
        byte[] asPublic = EcKeys.encode((ECPublicKey) ephemeral.getPublic());

        try {
            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(ephemeral.getPrivate());
            agreement.doPhase(uaKey, true);
            byte[] ecdhSecret = agreement.generateSecret();

            // HKDF with a single 32-byte block: Expand(PRK, info, L) == HMAC(PRK, info || 0x01) truncated to L.
            byte[] prkKey = hmac(authSecret, ecdhSecret);
            byte[] ikm = hmac(prkKey, concat(KEY_INFO_PREFIX, uaPublic, asPublic, new byte[]{1}));
            byte[] prk = hmac(salt, ikm);
            byte[] cek = Arrays.copyOf(hmac(prk, CEK_INFO), 16);
            byte[] nonce = Arrays.copyOf(hmac(prk, NONCE_INFO), 12);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(TAG_LENGTH * 8, nonce));
            // 0x02 marks the last (and only) record; no further padding.
            byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[]{2}));

            return ByteBuffer.allocate(HEADER_LENGTH + ciphertext.length)
                .put(salt)
                .putInt(RECORD_SIZE)
                .put((byte) asPublic.length)
                .put(asPublic)
                .put(ciphertext)
                .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Web Push encryption failed", e);
        }
    }

    static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(length);
        for (byte[] part : parts) {
            buffer.put(part);
        }
        return buffer.array();
    }
}
