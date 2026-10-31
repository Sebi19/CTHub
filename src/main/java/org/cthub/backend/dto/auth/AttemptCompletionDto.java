package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;
import org.cthub.backend.auth.attempt.LoginAttemptPurpose;

/** @param user the signed-in user (for {@code LOGIN}) or the current one (for {@code STEP_UP}) */
public record AttemptCompletionDto(@NotNull LoginAttemptPurpose purpose, @NotNull UserDto user) {
}