package org.cthub.backend.notification.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/** Encrypts a payload for one subscription and POSTs it to the push service (RFC 8030). */
@Component
@Slf4j
public class WebPushSender {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    /** RFC 8030 topics: at most 32 characters of the URL-safe base64 alphabet. */
    private static final Pattern TOPIC = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final VapidAuth vapid;
    private final HttpClient http;

    @Autowired
    public WebPushSender(VapidAuth vapid) {
        this(vapid, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
    }

    WebPushSender(VapidAuth vapid, HttpClient http) {
        this.vapid = vapid;
        this.http = http;
    }

    /**
     * @param topic optional; the push service replaces an undelivered message with the same topic,
     *              so an offline device only gets the latest one
     */
    public CompletableFuture<PushResult> send(PushTarget target, byte[] payload, Duration ttl, String topic) {
        byte[] body;
        URI endpoint;
        try {
            endpoint = URI.create(target.endpoint());
            body = WebPushEncryptor.encrypt(payload, EcKeys.decode(target.p256dh()), EcKeys.decode(target.auth()));
        } catch (IllegalArgumentException e) {
            log.warn("Push subscription {} is unusable and will be removed: {}", target.id(), e.getMessage());
            return CompletableFuture.completedFuture(PushResult.GONE);
        }

        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", vapid.authorization(endpoint))
            .header("TTL", Long.toString(Math.max(0, ttl.toSeconds())))
            .header("Urgency", "normal")
            .header("Content-Encoding", "aes128gcm")
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (topic != null && TOPIC.matcher(topic).matches()) {
            request.header("Topic", topic);
        }

        return http.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> classify(target, response))
            .exceptionally(e -> {
                log.warn("Push to subscription {} failed: {}", target.id(), e.toString());
                return PushResult.FAILED;
            });
    }

    private static PushResult classify(PushTarget target, HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return PushResult.DELIVERED;
        }
        if (status == 404 || status == 410) {
            return PushResult.GONE;
        }
        String detail = response.body() == null ? "" : response.body().strip();
        if (detail.length() > 300) {
            detail = detail.substring(0, 300) + "…";
        }
        if (status == 429 || status >= 500) {
            log.warn("Push service unavailable for subscription {} (HTTP {}): {}", target.id(), status, detail);
            return PushResult.FAILED;
        }
        log.warn("Push service rejected subscription {} (HTTP {}): {}", target.id(), status, detail);
        return PushResult.REJECTED;
    }
}
