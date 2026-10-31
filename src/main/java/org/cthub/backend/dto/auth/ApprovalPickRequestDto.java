package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ApprovalPickRequestDto(@Min(0) @Max(99) int number) {
}