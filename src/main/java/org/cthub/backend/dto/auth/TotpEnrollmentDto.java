package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

/** @param otpauthUri render as a QR code; {@code secret} is the manual-entry fallback */
public record TotpEnrollmentDto(@NotNull String secret, @NotNull String otpauthUri) {
}