package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkApproveRequestDto(
    @NotBlank @Size(max = 128) String token,
    @Min(0) @Max(99) int number
) {
}