package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The single e-mail entry point for signing in and signing up.
 *
 * @param pushEndpoint this browser's own push endpoint, if it has one, so it is not asked to approve itself
 * @param locale       UI language as a BCP-47 tag; used for the mail if a new account results
 */
public record IdentifyRequestDto(
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 2048) String pushEndpoint,
    @Size(max = 35) String locale
) {
}