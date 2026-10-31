package org.cthub.backend.model;

/**
 * Purpose of a {@link UserToken}. Sign-in, registration and step-up links are not user tokens; they
 * run through {@code LoginAttempt}.
 */
public enum UserTokenType {
    /** Confirms a requested e-mail change; mailed to the new address, which travels in the payload. */
    EMAIL_CHANGE
}
