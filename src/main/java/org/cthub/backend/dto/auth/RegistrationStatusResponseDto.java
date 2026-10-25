package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Polled by the registration-pending page. Carries no authentication side effects: a session is only
 * ever handed over through an explicit claim, and only after the confirmation challenge was answered
 * correctly by whoever holds the e-mailed link.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationStatusResponseDto {

    /** Distinct simple name: nested enums are exported to OpenAPI by simple name and must not collide. */
    public enum RegistrationStatus {
        /** Still waiting for the user to click the link in their inbox. */
        PENDING,
        /** The link was opened somewhere and that device is picking the number. */
        AWAITING_CONFIRMATION,
        /** Confirmed here: the client may now claim its session. */
        VERIFIED,
        /** Whoever opened the link chose to continue there instead. */
        CLAIMED_ELSEWHERE,
        /** Rejected as "that wasn't me", or too many wrong picks. */
        CANCELLED,
        /** Unknown or expired handle - the client should start over. */
        UNKNOWN
    }

    @NotNull
    private RegistrationStatus status;
}
