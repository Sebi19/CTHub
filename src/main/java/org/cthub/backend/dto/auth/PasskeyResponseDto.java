package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The browser's WebAuthn response, serialized with {@code JSON.stringify} (the WebAuthn Level 3
 * {@code toJSON()} shape that {@code @simplewebauthn/browser} returns).
 */
public record PasskeyResponseDto(
    @NotBlank @Size(max = 32_768) String credentialJson
) {
}
