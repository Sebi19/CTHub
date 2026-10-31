package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * {@code PublicKeyCredentialRequestOptionsJSON}. {@code allowCredentials} is always empty: passkeys
 * are discoverable, which also powers autofill (conditional mediation) before an e-mail is typed.
 */
public record PasskeyAuthenticationOptionsDto(
    @NotNull String challenge,
    @NotNull String rpId,
    long timeout,
    @NotNull String userVerification,
    @NotNull List<PasskeyDescriptorDto> allowCredentials
) {
}
