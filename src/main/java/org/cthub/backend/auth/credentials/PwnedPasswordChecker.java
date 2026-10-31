package org.cthub.backend.auth.credentials;

/** Checks whether a password appears in known data breaches. */
public interface PwnedPasswordChecker {

    /**
     * @return {@code true} if the password is known to be breached; {@code false} if it is not, or
     *         if the check could not be performed (fail-open: an outage must not block users)
     */
    boolean isPwned(String password);
}
