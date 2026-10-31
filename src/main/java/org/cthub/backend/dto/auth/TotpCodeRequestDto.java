package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TotpCodeRequestDto(@NotBlank @Size(max = 16) String code) {
}