package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

public record ChallengePickResultDto(@NotNull ChallengePickResult result) {
}