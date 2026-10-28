package org.cthub.backend.notification.live;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Holds the open server-sent-event streams of this instance, keyed by user.
 *
 * <p>Delivery is best-effort: anything a client misses while disconnected is replayed over REST
 * ({@code GET /api/notifications/replay}) when its stream reopens. Connections are kept in memory,
 * which assumes a single backend instance; scaling out would need a fan-out (e.g. Postgres
 * {@code LISTEN/NOTIFY}) in front of {@link #dispatch}.
 *
 * <p>Sends never run on the caller's thread, so a stalled client cannot block a request or a
 * transaction. Work is striped by user id onto single-threaded lanes, which keeps the events of
 * one user in order while isolating a slow client to its own lane.
 */
@Component
@Slf4j
public class LiveNotificationBroker {

    static final Duration EMITTER_TIMEOUT = Duration.ofMinutes(30);
    static final int MAX_CONNECTIONS_PER_USER = 10;
    private static final int LANES = 4;
    private static final int LANE_QUEUE_CAPACITY = 1_000;

    private final Map<Long, Set<Connection>> connections = new ConcurrentHashMap<>();
    private final List<ExecutorService> lanes = new ArrayList<>(LANES);

    public LiveNotificationBroker() {
        for (int i = 0; i < LANES; i++) {
            int lane = i;
            lanes.add(new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(LANE_QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "notification-live-" + lane);
                    thread.setDaemon(true);
                    return thread;
                }));
        }
    }

    /** Opens a stream. Must not touch the database: the request stays open for the whole stream. */
    public SseEmitter connect(long userId, String sessionId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT.toMillis());
        Connection connection = new Connection(userId, sessionId, emitter);

        List<Connection> evicted = new ArrayList<>();
        connections.compute(userId, (id, set) -> {
            Set<Connection> userConnections = set != null ? set : new CopyOnWriteArraySet<>();
            userConnections.add(connection);
            for (Connection existing : userConnections) {
                if (userConnections.size() <= MAX_CONNECTIONS_PER_USER) {
                    break;
                }
                userConnections.remove(existing);
                evicted.add(existing);
            }
            return userConnections;
        });
        evicted.forEach(old -> old.emitter().complete());

        emitter.onCompletion(() -> remove(connection));
        emitter.onTimeout(() -> remove(connection));
        emitter.onError(error -> remove(connection));

        // Flushes the response headers so the browser's EventSource reports "open" right away.
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok").reconnectTime(5_000));
        } catch (IOException e) {
            remove(connection);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public boolean isConnected(long userId) {
        Set<Connection> userConnections = connections.get(userId);
        return userConnections != null && !userConnections.isEmpty();
    }

    /**
     * Runs {@code work} on the user's lane if they have at least one open stream. The work receives
     * a {@link Sender} that writes to all of the user's streams.
     */
    public void dispatch(long userId, Consumer<Sender> work) {
        if (!isConnected(userId)) {
            return;
        }
        try {
            lane(userId).execute(() -> {
                try {
                    work.accept((name, data) -> sendToUser(userId, name, data));
                } catch (RuntimeException e) {
                    log.warn("Live notification dispatch for user {} failed: {}", userId, e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("Live notification lane is full; dropping event for user {} (client will replay).", userId);
        }
    }

    /** Ends the streams opened from the given HTTP session, e.g. on logout. Other devices stay connected. */
    public void closeSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        connections.values().forEach(set -> set.stream()
            .filter(connection -> sessionId.equals(connection.sessionId()))
            .forEach(connection -> {
                remove(connection);
                connection.emitter().complete();
            }));
    }

    /** Keeps idle streams alive through proxies and detects dead clients. */
    @Scheduled(fixedDelay = 25, timeUnit = TimeUnit.SECONDS)
    public void heartbeat() {
        for (Long userId : List.copyOf(connections.keySet())) {
            try {
                lane(userId).execute(() -> sendToUser(userId, null, null));
            } catch (RejectedExecutionException e) {
                log.debug("Skipping heartbeat for user {}: lane is full.", userId);
            }
        }
    }

    int connectionCount() {
        return connections.values().stream().mapToInt(Collection::size).sum();
    }

    @PreDestroy
    void shutdown() {
        lanes.forEach(ExecutorService::shutdownNow);
        connections.values().forEach(set -> set.forEach(connection -> connection.emitter().complete()));
        connections.clear();
    }

    private void sendToUser(long userId, String name, Object data) {
        Set<Connection> userConnections = connections.get(userId);
        if (userConnections == null) {
            return;
        }
        for (Connection connection : userConnections) {
            try {
                if (name == null) {
                    connection.emitter().send(SseEmitter.event().comment("ping"));
                } else {
                    connection.emitter().send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
                }
            } catch (IOException | IllegalStateException e) {
                // The client went away; the container will also fire onError/onCompletion.
                remove(connection);
                connection.emitter().completeWithError(e);
            }
        }
    }

    private void remove(Connection connection) {
        connections.computeIfPresent(connection.userId(), (id, set) -> {
            set.remove(connection);
            return set.isEmpty() ? null : set;
        });
    }

    private ExecutorService lane(long userId) {
        return lanes.get(Math.floorMod(Long.hashCode(userId), LANES));
    }

    @FunctionalInterface
    public interface Sender {
        void send(String eventName, Object data);
    }

    private record Connection(long userId, String sessionId, SseEmitter emitter) {
        // Identity semantics: two tabs of the same session are distinct connections.
        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
