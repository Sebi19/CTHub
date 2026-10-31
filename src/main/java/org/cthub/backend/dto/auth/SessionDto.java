package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;
import org.cthub.backend.auth.session.AuthMethod;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionDto(
    @NotNull UUID id,
    String device,
    @NotNull Instant createdAt,
    @NotNull Instant lastUsedAt,
    boolean current,
    @NotNull List<AuthMethod> methods
) {
}