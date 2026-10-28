package org.cthub.backend.notification.push;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VapidAuthTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    static PushProperties properties(KeyPair keys) {
        PushProperties properties = new PushProperties();
        properties.getVapid().setPublicKey(EcKeys.encode(EcKeys.encode((ECPublicKey) keys.getPublic())));
        properties.getVapid().setPrivateKey(EcKeys.encode(EcKeys.encode((ECPrivateKey) keys.getPrivate())));
        properties.getVapid().setSubject("mailto:test@example.org");
        return properties;
    }

    @Test
    void signsAVerifiableEs256TokenForTheEndpointOrigin() throws Exception {
        KeyPair keys = EcKeys.generate();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        VapidAuth vapid = new VapidAuth(properties(keys), Clock.fixed(now, ZoneOffset.UTC));

        String header = vapid.authorization(URI.create("https://fcm.googleapis.com/fcm/send/abc"));

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + vapid.publicKey());
        String jwt = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);

        JsonNode head = JSON.readTree(EcKeys.decode(parts[0]));
        assertThat(head.get("alg").asString()).isEqualTo("ES256");
        JsonNode claims = JSON.readTree(EcKeys.decode(parts[1]));
        assertThat(claims.get("aud").asString()).isEqualTo("https://fcm.googleapis.com");
        assertThat(claims.get("sub").asString()).isEqualTo("mailto:test@example.org");
        assertThat(claims.get("exp").asLong()).isEqualTo(now.plus(VapidAuth.TOKEN_LIFETIME).getEpochSecond());

        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(keys.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(EcKeys.decode(parts[2]))).isTrue();
    }

    @Test
    void reusesTokensPerOriginUntilCloseToExpiry() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        VapidAuth vapid = new VapidAuth(properties(EcKeys.generate()), clock);
        URI fcm = URI.create("https://fcm.googleapis.com/fcm/send/a");

        String first = vapid.authorization(fcm);
        assertThat(vapid.authorization(URI.create("https://fcm.googleapis.com/fcm/send/b"))).isEqualTo(first);
        assertThat(vapid.authorization(URI.create("https://updates.push.services.mozilla.com/wpush/v2/x"))).isNotEqualTo(first);

        clock.now = clock.now.plus(Duration.ofHours(10));
        assertThat(vapid.authorization(fcm)).isEqualTo(first);
        clock.now = clock.now.plus(Duration.ofHours(1).plusSeconds(1));
        assertThat(vapid.authorization(fcm)).isNotEqualTo(first);
    }

    @Test
    void isDisabledWithoutKeys() {
        VapidAuth vapid = new VapidAuth(new PushProperties());

        assertThat(vapid.isEnabled()).isFalse();
        assertThat(vapid.publicKey()).isNull();
        assertThatThrownBy(() -> vapid.authorization(URI.create("https://fcm.googleapis.com/x")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesMismatchedOrMalformedKeys() {
        PushProperties mismatched = properties(EcKeys.generate());
        mismatched.getVapid().setPublicKey(properties(EcKeys.generate()).getVapid().getPublicKey());
        assertThatThrownBy(() -> new VapidAuth(mismatched)).isInstanceOf(IllegalStateException.class);

        PushProperties malformed = properties(EcKeys.generate());
        malformed.getVapid().setPublicKey("not-a-key");
        assertThatThrownBy(() -> new VapidAuth(malformed)).isInstanceOf(IllegalStateException.class);

        PushProperties badSubject = properties(EcKeys.generate());
        badSubject.getVapid().setSubject("info@example.org");
        assertThatThrownBy(() -> new VapidAuth(badSubject)).isInstanceOf(IllegalStateException.class);
    }

    private static final class MutableClock extends Clock {
        Instant now;

        MutableClock(Instant now) {
            this.now = now;
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
}
