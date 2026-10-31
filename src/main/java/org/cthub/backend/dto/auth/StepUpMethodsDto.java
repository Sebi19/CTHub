package org.cthub.backend.dto.auth;

/** Ways the current user can re-confirm their identity; the push / e-mail challenge always works. */
public record StepUpMethodsDto(boolean passkey, boolean totp) {
}