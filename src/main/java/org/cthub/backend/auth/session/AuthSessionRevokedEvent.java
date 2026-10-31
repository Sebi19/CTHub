package org.cthub.backend.auth.session;

import java.util.UUID;

/**
 * Published (in the revoking transaction) when a session ends early. Listeners drop the device's
 * push subscriptions and close its live streams.
 */
public record AuthSessionRevokedEvent(UUID sessionId, long userId, SessionRevokeReason reason) {
}
