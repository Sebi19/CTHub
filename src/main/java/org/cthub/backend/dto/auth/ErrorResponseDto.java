package org.cthub.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Generic error body with a stable machine-readable {@code code} the frontend can branch on. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {
    private String code;
    private String message;
}
