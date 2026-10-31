package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** @param synced backed up to a passkey provider (e.g. iCloud Keychain, Google Password Manager) */
public record PasskeyDto(
    @NotNull Long id,
    @NotNull String name,
    @NotNull Instant createdAt,
    Instant lastUsedAt,
    boolean synced
) {
}