package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** @param totpAvailable whether this server is configured for authenticator apps at all */
public record AccountSecurityDto(
    boolean passwordSet,
    boolean totpEnabled,
    boolean totpAvailable,
    @NotNull List<PasskeyDto> passkeys
) {
}