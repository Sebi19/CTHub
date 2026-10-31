package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * {@code PublicKeyCredentialCreationOptionsJSON} as understood by {@code navigator.credentials.create}
 * (via {@code @simplewebauthn/browser}). All binary values are base64url.
 */
public record PasskeyRegistrationOptionsDto(
    @NotNull PasskeyRpDto rp,
    @NotNull PasskeyUserDto user,
    @NotNull String challenge,
    @NotNull List<PasskeyCredentialParamDto> pubKeyCredParams,
    long timeout,
    @NotNull List<PasskeyDescriptorDto> excludeCredentials,
    @NotNull PasskeyAuthenticatorSelectionDto authenticatorSelection,
    @NotNull String attestation
) {
    public record PasskeyRpDto(@NotNull String id, @NotNull String name) {
    }

    public record PasskeyUserDto(@NotNull String id, @NotNull String name, @NotNull String displayName) {
    }

    public record PasskeyCredentialParamDto(@NotNull String type, int alg) {
    }

    public record PasskeyAuthenticatorSelectionDto(@NotNull String residentKey, boolean requireResidentKey,
                                                   @NotNull String userVerification) {
    }
}
