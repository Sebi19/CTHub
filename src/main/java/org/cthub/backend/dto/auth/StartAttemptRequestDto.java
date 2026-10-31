package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Skips passkey/password and goes straight to the push / e-mail challenge. */
public record StartAttemptRequestDto(
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 2048) String pushEndpoint,
    @Size(max = 35) String locale
) {
}