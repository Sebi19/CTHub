package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of starting a registration.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationStartResponseDto {

    /** Distinct simple name: nested enums are exported to OpenAPI by simple name and must not collide. */
    public enum RegistrationStartStatus {
        /** Confirmation mail sent; poll {@code /registration-status} with the returned id. */
        PENDING,
        /** A verified account already owns this address - offer login instead. */
        ALREADY_REGISTERED
    }

    @NotNull
    private RegistrationStartStatus status;

    /** Opaque polling handle. {@code null} when {@link #status} is {@code ALREADY_REGISTERED}. */
    private String registrationId;

    /**
     * Two-digit number this browser must display: whoever opens the e-mailed link picks it out of
     * three to hand the session back here. {@code null} when {@code ALREADY_REGISTERED}.
     */
    private Short challengeNumber;
}
