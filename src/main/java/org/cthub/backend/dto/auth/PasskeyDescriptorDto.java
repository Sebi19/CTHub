package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** A credential reference in WebAuthn options ({@code PublicKeyCredentialDescriptorJSON}). */
public record PasskeyDescriptorDto(@NotNull String id, @NotNull String type, List<String> transports) {
}
