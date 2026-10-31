package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeRequestDto(@NotBlank @Email @Size(max = 255) String newEmail) {
}