package org.cthub.backend.notification.push;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebPushEncryptorTest {

    /** RFC 8291, Appendix A. */
    @Test
    void matchesTheRfcTestVector() {
        byte[] plaintext = EcKeys.decode("V2hlbiBJIGdyb3cgdXAsIEkgd2FudCB0byBiZSBhIHdhdGVybWVsb24");
        KeyPair asKeys = new KeyPair(
            EcKeys.publicKey(EcKeys.decode("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8")),
            EcKeys.privateKey(EcKeys.decode("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw")));
        byte[] uaPublic = EcKeys.decode("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] salt = EcKeys.decode("DGv6ra1nlYgDCS1FRnbzlw");
        byte[] auth = EcKeys.decode("BTBZMqHH6r4Tts7J_aSIgg");

        byte[] body = WebPushEncryptor.encrypt(plaintext, uaPublic, auth, asKeys, salt);

        assertThat(body).isEqualTo(WebPushEncryptor.concat(
            EcKeys.decode("DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8"),
            EcKeys.decode("8pfeW0KbunFT06SuDKoJH9Ql87S1QUrdirN6GcG7sFz1y1sqLgVi1VhjVkHsUoEsbI_0LpXMuGvnzQ")));
    }

    @Test
    void browserCanDecryptARandomlyKeyedMessage() throws Exception {
        KeyPair browser = EcKeys.generate();
        byte[] uaPublic = EcKeys.encode((ECPublicKey) browser.getPublic());
        byte[] auth = new byte[16];
        Arrays.fill(auth, (byte) 7);
        byte[] plaintext = "{\"title\":\"Hallo Welt – ünïcödé\"}".getBytes(StandardCharsets.UTF_8);

        byte[] body = WebPushEncryptor.encrypt(plaintext, uaPublic, auth);

        assertThat(decrypt(body, (ECPrivateKey) browser.getPrivate(), uaPublic, auth)).isEqualTo(plaintext);
        assertThat(body).isNotEqualTo(WebPushEncryptor.encrypt(plaintext, uaPublic, auth));
    }

    @Test
    void rejectsOversizedPayloadsAndBadSecrets() {
        byte[] uaPublic = EcKeys.encode((ECPublicKey) EcKeys.generate().getPublic());
        assertThatThrownBy(() -> WebPushEncryptor.encrypt(new byte[WebPushEncryptor.MAX_PLAINTEXT + 1], uaPublic, new byte[16]))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WebPushEncryptor.encrypt(new byte[10], uaPublic, new byte[15]))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(WebPushEncryptor.encrypt(new byte[WebPushEncryptor.MAX_PLAINTEXT], uaPublic, new byte[16]))
            .hasSize(WebPushEncryptor.RECORD_SIZE);
    }

    /** What the browser does (RFC 8291 section 3.4 / RFC 8188), independent of the production code path. */
    private static byte[] decrypt(byte[] body, ECPrivateKey uaPrivate, byte[] uaPublic, byte[] auth) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(body);
        byte[] salt = new byte[16];
        buffer.get(salt);
        assertThat(buffer.getInt()).isEqualTo(4096);
        byte[] asPublic = new byte[buffer.get()];
        buffer.get(asPublic);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(uaPrivate);
        agreement.doPhase(EcKeys.publicKey(asPublic), true);
        byte[] ecdh = agreement.generateSecret();

        byte[] prkKey = WebPushEncryptor.hmac(auth, ecdh);
        byte[] ikm = WebPushEncryptor.hmac(prkKey, WebPushEncryptor.concat(
            "WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic, new byte[]{1}));
        byte[] prk = WebPushEncryptor.hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(WebPushEncryptor.hmac(prk, "Content-Encoding: aes128gcm\0\1".getBytes(StandardCharsets.US_ASCII)), 16);
        byte[] nonce = Arrays.copyOf(WebPushEncryptor.hmac(prk, "Content-Encoding: nonce\0\1".getBytes(StandardCharsets.US_ASCII)), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] padded = cipher.doFinal(ciphertext);
        assertThat(padded[padded.length - 1]).isEqualTo((byte) 2);
        return Arrays.copyOf(padded, padded.length - 1);
    }
}
