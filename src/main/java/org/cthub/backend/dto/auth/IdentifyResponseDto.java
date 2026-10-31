package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

/**
 * Passkey and password are only revealed for accounts that opted into them; any other address -
 * passwordless or not registered at all - gets an identical {@code CHALLENGE} response.
 *
 * @param passwordAvailable whether "use password instead" may be offered next to the passkey prompt
 * @param attempt           set for {@link AuthNextStep#CHALLENGE}
 */
public record IdentifyResponseDto(
    @NotNull AuthNextStep next,
    boolean passwordAvailable,
    LoginAttemptStateDto attempt
) {
}