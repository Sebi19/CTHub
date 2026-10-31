package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Length and breach checks happen server-side (PasswordPolicy) so the messages stay specific. */
public record SetPasswordRequestDto(@NotBlank @Size(max = 256) String password) {
}