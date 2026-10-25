package org.cthub.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response body for registration/verification endpoints. Intentionally carries no detail
 * that could be used to enumerate whether an e-mail address is already registered.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationResponseDto {
    private String message;
}
