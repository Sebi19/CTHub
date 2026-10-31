package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;
import org.cthub.backend.auth.attempt.LoginAttemptPurpose;

import java.time.Instant;

/**
 * Deliberately says nothing about which channels were used, so it cannot reveal whether the
 * address has an account with push-enabled devices.
 *
 * @param openedElsewhere the link or notification was opened on another device
 */
public record LoginAttemptStateDto(
    @NotNull LoginAttemptState state,
    @NotNull LoginAttemptPurpose purpose,
    int challengeNumber,
    @NotNull Instant expiresAt,
    boolean openedElsewhere
) {
}