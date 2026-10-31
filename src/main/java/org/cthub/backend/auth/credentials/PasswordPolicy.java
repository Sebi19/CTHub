package org.cthub.backend.auth.credentials;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.exception.ApiException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * NIST SP 800-63B style: a length range and a breach check, no composition rules. The upper bound
 * keeps every password within BCrypt's 72-byte input limit, beyond which bytes are silently ignored.
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 64;
    private static final int MAX_BYTES = 72;

    private final PwnedPasswordChecker pwnedPasswordChecker;

    public void validate(String password) {
        if (password == null || password.codePointCount(0, password.length()) < MIN_LENGTH) {
            throw ApiException.badRequest("PASSWORD_TOO_SHORT",
                "The password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (password.codePointCount(0, password.length()) > MAX_LENGTH
            || password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw ApiException.badRequest("PASSWORD_TOO_LONG",
                "The password must be at most " + MAX_LENGTH + " characters long.");
        }
        if (pwnedPasswordChecker.isPwned(password)) {
            throw ApiException.badRequest("PASSWORD_PWNED",
                "This password appeared in a data breach. Please choose a different one.");
        }
    }
}
