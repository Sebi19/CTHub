package org.cthub.backend.notification.push;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushSenderTest {

    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(201);
    private final Map<String, List<String>> lastHeaders = new ConcurrentHashMap<>();
    private volatile byte[] lastBody;
    private WebPushSender sender;
    private PushTarget target;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/push", exchange -> {
            lastHeaders.clear();
            exchange.getRequestHeaders().forEach((k, v) -> lastHeaders.put(k.toLowerCase(), v));
            lastBody = exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();

        sender = new WebPushSender(new VapidAuth(VapidAuthTest.properties(EcKeys.generate())));
        byte[] uaPublic = EcKeys.encode((ECPublicKey) EcKeys.generate().getPublic());
        target = new PushTarget(1, 2, "http://127.0.0.1:" + server.getAddress().getPort() + "/push/abc",
            EcKeys.encode(uaPublic), EcKeys.encode(new byte[16]));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void postsAnEncryptedBodyWithWebPushHeaders() {
        PushResult result = sender.send(target, "{\"title\":\"x\"}".getBytes(), Duration.ofHours(2), "cthub-ANNOUNCEMENT").join();

        assertThat(result).isEqualTo(PushResult.DELIVERED);
        assertThat(lastHeaders.get("authorization").getFirst()).startsWith("vapid t=");
        assertThat(lastHeaders.get("ttl")).containsExactly("7200");
        assertThat(lastHeaders.get("content-encoding")).containsExactly("aes128gcm");
        assertThat(lastHeaders.get("topic")).containsExactly("cthub-ANNOUNCEMENT");
        assertThat(lastBody.length).isGreaterThan(86 + 16);
    }

    @Test
    void omitsInvalidTopics() {
        sender.send(target, new byte[1], Duration.ofHours(1), "not valid!").join();
        assertThat(lastHeaders).doesNotContainKey("topic");
    }

    @Test
    void mapsPushServiceResponses() {
        assertThat(resultFor(404)).isEqualTo(PushResult.GONE);
        assertThat(resultFor(410)).isEqualTo(PushResult.GONE);
        assertThat(resultFor(429)).isEqualTo(PushResult.FAILED);
        assertThat(resultFor(503)).isEqualTo(PushResult.FAILED);
        assertThat(resultFor(400)).isEqualTo(PushResult.REJECTED);
        assertThat(resultFor(403)).isEqualTo(PushResult.REJECTED);
    }

    @Test
    void unreachableServiceIsAFailureAndBrokenKeysAreGone() {
        server.stop(0);
        assertThat(sender.send(target, new byte[1], Duration.ofHours(1), null).join()).isEqualTo(PushResult.FAILED);

        PushTarget broken = new PushTarget(1, 2, target.endpoint(), "AAAA", target.auth());
        assertThat(sender.send(broken, new byte[1], Duration.ofHours(1), null).join()).isEqualTo(PushResult.GONE);
    }

    private PushResult resultFor(int code) {
        status.set(code);
        return sender.send(target, new byte[1], Duration.ofHours(1), null).join();
    }
}
