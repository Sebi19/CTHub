package org.cthub.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * What the device opening a verification link should be shown.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationChallengeResponseDto {

    /** Distinct simple name: nested enums are exported to OpenAPI by simple name and must not collide. */
    public enum ChallengeKind {
        /** Ask which number the originating browser is displaying. */
        CHALLENGE,
        /** Opened in the browser that started the registration - verify straight away. */
        SAME_BROWSER,
        /** No browser is waiting on this registration - verify straight away. */
        NO_CHALLENGE,
        /** Token expired, already used, or the attempt is settled. */
        INVALID
    }

    private ChallengeKind kind;

    /** The three options, shuffled. Only present for {@link ChallengeKind#CHALLENGE}. */
    private List<Short> numbers;

    /** Address the registration was started for, so the opener can recognise it. */
    private String email;
}
