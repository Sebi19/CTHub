package org.cthub.backend.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private final RateLimiterService rateLimiterService = new RateLimiterService();

    @Test
    void allowsCallsUpToMaxAttemptsWithinWindow() {
        String key = "test-key-1";
        assertThat(rateLimiterService.tryAcquire(key, 3, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiterService.tryAcquire(key, 3, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiterService.tryAcquire(key, 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void rejectsCallsBeyondMaxAttemptsWithinWindow() {
        String key = "test-key-2";
        rateLimiterService.tryAcquire(key, 2, Duration.ofMinutes(1));
        rateLimiterService.tryAcquire(key, 2, Duration.ofMinutes(1));

        assertThat(rateLimiterService.tryAcquire(key, 2, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void differentKeysHaveIndependentBudgets() {
        rateLimiterService.tryAcquire("key-a", 1, Duration.ofMinutes(1));

        assertThat(rateLimiterService.tryAcquire("key-b", 1, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void blockVetoesAKeyRegardlessOfAttemptCount() {
        assertThat(rateLimiterService.isBlocked("blocked-key")).isFalse();

        rateLimiterService.block("blocked-key", Duration.ofMinutes(5));

        assertThat(rateLimiterService.isBlocked("blocked-key")).isTrue();
        assertThat(rateLimiterService.isBlocked("other-key")).isFalse();
    }

    @Test
    void blockLapsesOnceItsDurationHasPassed() throws InterruptedException {
        rateLimiterService.block("short-block", Duration.ofMillis(50));
        Thread.sleep(120);

        assertThat(rateLimiterService.isBlocked("short-block")).isFalse();
    }
}
