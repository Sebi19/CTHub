package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkTokenRequestDto(@NotBlank @Size(max = 128) String token) {
}