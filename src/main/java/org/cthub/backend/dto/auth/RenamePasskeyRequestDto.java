package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenamePasskeyRequestDto(@NotBlank @Size(max = 64) String name) {
}