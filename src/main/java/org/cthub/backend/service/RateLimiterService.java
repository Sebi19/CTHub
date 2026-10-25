package org.cthub.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory fixed-window rate limiter backed by Caffeine. Good enough for a single-instance
 * deployment; keys expire automatically after the window elapses so no manual cleanup is needed.
 * Not suitable for a multi-instance deployment without a shared store, but that's out of scope for now.
 */
@Service
public class RateLimiterService {

    private final Cache<String, AtomicInteger> buckets = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .maximumSize(100_000)
        .build();

    /**
     * Separate from {@link #buckets} because a block is a deliberate, timed veto rather than a
     * count: entries carry their own expiry so each block lasts exactly as long as it was asked for.
     */
    private final Cache<String, Instant> blocks = Caffeine.newBuilder()
        .expireAfter(new Expiry<String, Instant>() {
            @Override
            public long expireAfterCreate(String key, Instant until, long currentTime) {
                return Math.max(0, Duration.between(Instant.now(), until).toNanos());
            }

            @Override
            public long expireAfterUpdate(String key, Instant until, long currentTime, long currentDuration) {
                return expireAfterCreate(key, until, currentTime);
            }

            @Override
            public long expireAfterRead(String key, Instant until, long currentTime, long currentDuration) {
                return currentDuration;
            }
        })
        .maximumSize(100_000)
        .build();

    /**
     * Vetoes {@code key} outright for {@code duration}, regardless of how many attempts were made.
     * Used where a single event should impose a cooling-off period rather than consume a quota.
     */
    public void block(String key, Duration duration) {
        blocks.put(key, Instant.now().plus(duration));
    }

    /** Returns {@code true} while a {@link #block(String, Duration)} on {@code key} is still in force. */
    public boolean isBlocked(String key) {
        return blocks.getIfPresent(key) != null;
    }

    /**
     * Returns {@code true} if the caller identified by {@code key} may proceed, i.e. has not yet
     * made {@code maxAttempts} calls within the current {@code window}. Each call that returns
     * {@code true} counts towards the limit.
     */
    public boolean tryAcquire(String key, int maxAttempts, Duration window) {
        String windowedKey = key + ":" + (System.currentTimeMillis() / window.toMillis());
        AtomicInteger counter = buckets.get(windowedKey, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= maxAttempts;
    }
}
