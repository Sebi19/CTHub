package org.cthub.backend.dto.auth;

public enum LoginResultStatus {
    SIGNED_IN,
    /** Password accepted; send the authenticator code to {@code /api/auth/login/password/totp}. */
    TOTP_REQUIRED
}