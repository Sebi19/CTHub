package org.cthub.backend.notification.push;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.notification.NotificationPreferenceService;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Resolves who gets a push for new notifications and sends them. Runs on its own single thread so
 * neither the committing request nor the shared {@code mailExecutor} waits on push services.
 *
 * <p>Delivery is best effort, like the live stream: the in-app notification is the source of truth,
 * and a push that arrives hours late is worse than none. Transient failures are logged, not retried.
 * Subscriptions the push service reports as gone are deleted.
 */
@Component
@Slf4j
public class PushDispatcher {

    private static final int LOOKUP_CHUNK = 1000;
    private static final int QUEUE_CAPACITY = 500;
    private static final long BATCH_TIMEOUT_SECONDS = 120;
    private static final long TEST_TIMEOUT_SECONDS = 20;
    private static final Duration DISMISS_TTL = Duration.ofMinutes(15);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final VapidAuth vapid;
    private final PushProperties properties;
    private final PushSubscriptionRepository repository;
    private final NotificationPreferenceService preferenceService;
    private final PushMessageComposer composer;
    private final WebPushSender sender;
    private final ThreadPoolExecutor executor;

    public PushDispatcher(VapidAuth vapid, PushProperties properties, PushSubscriptionRepository repository,
                          NotificationPreferenceService preferenceService, PushMessageComposer composer,
                          WebPushSender sender) {
        this.vapid = vapid;
        this.properties = properties;
        this.repository = repository;
        this.preferenceService = preferenceService;
        this.composer = composer;
        this.sender = sender;
        AtomicInteger threads = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            runnable -> {
                Thread thread = new Thread(runnable, "push-dispatch-" + threads.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            },
            (runnable, pool) -> log.warn("Push queue is full; dropping a batch of push notifications."));
    }

    /** Queues delivery and returns immediately. */
    public void dispatch(List<NotificationsCreatedEvent.Item> items) {
        if (!vapid.isEnabled() || items.isEmpty()) {
            return;
        }
        executor.execute(() -> {
            try {
                deliver(items);
            } catch (RuntimeException e) {
                log.error("Push delivery failed for {} notification(s).", items.size(), e);
            }
        });
    }

    /** Synchronous delivery; {@link #dispatch} runs this on the push thread. */
    void deliver(List<NotificationsCreatedEvent.Item> items) {
        Map<NotificationType, List<NotificationsCreatedEvent.Item>> byType = items.stream()
            .filter(item -> item.type().supports(NotificationChannel.PUSH))
            .collect(Collectors.groupingBy(NotificationsCreatedEvent.Item::type));

        List<CompletableFuture<Void>> sends = new ArrayList<>();
        byType.forEach((type, typeItems) -> {
            Set<Long> recipients = preferenceService.recipientsFor(type, NotificationChannel.PUSH,
                typeItems.stream().map(NotificationsCreatedEvent.Item::userId).toList());
            if (recipients.isEmpty()) {
                return;
            }
            Map<Long, List<PushTarget>> targets = targetsFor(recipients);
            for (NotificationsCreatedEvent.Item item : typeItems) {
                List<PushTarget> devices = targets.getOrDefault(item.userId(), List.of());
                if (devices.isEmpty()) {
                    continue;
                }
                byte[] payload;
                PushMessage message;
                try {
                    message = composer.compose(item);
                    payload = JSON.writeValueAsBytes(message);
                } catch (RuntimeException e) {
                    log.error("Could not build push for notification {} ({}).", item.id(), item.type(), e);
                    continue;
                }
                devices.forEach(device -> sends.add(send(device, payload, message.tag())));
            }
        });
        await(sends, BATCH_TIMEOUT_SECONDS);
    }

    /**
     * Sends a test push to every device of the user and waits for the push services' answers.
     *
     * @return how many devices the push service accepted the message for, out of how many
     */
    public TestResult sendTest(long userId, Locale locale) {
        List<PushTarget> devices = targetsFor(Set.of(userId)).getOrDefault(userId, List.of());
        PushMessage message = composer.test(locale);
        byte[] payload = JSON.writeValueAsBytes(message);
        List<CompletableFuture<PushResult>> results = devices.stream()
            .map(device -> sendWithResult(device, payload, message.tag()))
            .toList();
        await(results, TEST_TIMEOUT_SECONDS);
        long delivered = results.stream()
            .filter(f -> f.isDone() && !f.isCompletedExceptionally() && f.join() == PushResult.DELIVERED)
            .count();
        return new TestResult(devices.size(), (int) delivered);
    }

    public record TestResult(int devices, int delivered) {
    }

    /** Live devices of {@code userId} that would receive a login approval, minus {@code excludedEndpoint}. */
    public List<PushTarget> loginApprovalTargets(long userId, String excludedEndpoint) {
        if (!vapid.isEnabled()) {
            return List.of();
        }
        return repository.findDeliverable(Set.of(userId), Instant.now()).stream()
            .filter(target -> excludedEndpoint == null || !excludedEndpoint.strip().equals(target.endpoint()))
            .toList();
    }

    /**
     * Fans a login approval request out to the given devices, urgently: somebody is waiting for it.
     * {@code message.tag()} doubles as the topic, so {@link #dismiss} can replace it if undelivered.
     */
    public void sendLoginApproval(List<PushTarget> targets, PushMessage message, Duration ttl) {
        if (!vapid.isEnabled() || targets.isEmpty()) {
            return;
        }
        byte[] payload = JSON.writeValueAsBytes(message);
        executor.execute(() -> await(targets.stream()
            .map(device -> sendWithResult(device, payload, message.tag(), ttl, WebPushSender.Urgency.HIGH))
            .toList(), BATCH_TIMEOUT_SECONDS));
    }

    /** Silently closes the login approval with this tag on every device of the user. */
    public void dismiss(long userId, String tag) {
        if (!vapid.isEnabled()) {
            return;
        }
        executor.execute(() -> {
            try {
                List<PushTarget> devices = repository.findDeliverable(Set.of(userId), Instant.now());
                byte[] payload = JSON.writeValueAsBytes(PushMessageComposer.dismiss(tag));
                await(devices.stream()
                    .map(device -> sendWithResult(device, payload, tag, DISMISS_TTL, WebPushSender.Urgency.HIGH))
                    .toList(), BATCH_TIMEOUT_SECONDS);
            } catch (RuntimeException e) {
                log.warn("Could not dismiss login approval {} for user {}.", tag, userId, e);
            }
        });
    }

    private CompletableFuture<Void> send(PushTarget device, byte[] payload, String topic) {
        return sendWithResult(device, payload, topic).thenAccept(result -> { });
    }

    private CompletableFuture<PushResult> sendWithResult(PushTarget device, byte[] payload, String topic) {
        return sender.send(device, payload, properties.getTtl(), topic)
            .thenApply(result -> handle(device, result));
    }

    private CompletableFuture<PushResult> sendWithResult(PushTarget device, byte[] payload, String topic,
                                                         Duration ttl, WebPushSender.Urgency urgency) {
        return sender.send(device, payload, ttl, topic, urgency)
            .thenApply(result -> handle(device, result));
    }

    private PushResult handle(PushTarget device, PushResult result) {
        if (result == PushResult.GONE) {
            removeGone(device);
        }
        return result;
    }

    private void removeGone(PushTarget device) {
        try {
            repository.deleteById(device.id());
            log.info("Removed expired push subscription {} of user {}.", device.id(), device.userId());
        } catch (RuntimeException e) {
            log.warn("Could not remove expired push subscription {}.", device.id(), e);
        }
    }

    private Map<Long, List<PushTarget>> targetsFor(Set<Long> userIds) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(userIds));
        List<PushTarget> targets = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += LOOKUP_CHUNK) {
            targets.addAll(repository.findDeliverable(ids.subList(i, Math.min(i + LOOKUP_CHUNK, ids.size())), Instant.now()));
        }
        return targets.stream().collect(Collectors.groupingBy(PushTarget::userId));
    }

    private static void await(List<? extends CompletableFuture<?>> futures, long timeoutSeconds) {
        if (futures.isEmpty()) {
            return;
        }
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Not all push requests completed: {}", e.toString());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
