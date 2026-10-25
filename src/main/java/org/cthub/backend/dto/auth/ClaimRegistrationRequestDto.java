package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries the originating browser's registration handle so it can claim its session once the
 * confirmation challenge was answered correctly elsewhere.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimRegistrationRequestDto {

    @NotBlank
    private String registrationId;
}
