package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;
import org.cthub.backend.auth.attempt.LoginAttemptPurpose;

import java.time.Instant;
import java.util.List;

/**
 * What the link landing page or the push approval screen shows.
 *
 * @param device      "Browser on OS" of the requesting device, if recognisable
 * @param options     the three numbers to pick from ({@link ChallengeViewKind#CROSS} only)
 */
public record ChallengeViewDto(
    @NotNull ChallengeViewKind kind,
    LoginAttemptPurpose purpose,
    @NotNull List<Integer> options,
    String device,
    Instant requestedAt,
    Instant expiresAt
) {
}