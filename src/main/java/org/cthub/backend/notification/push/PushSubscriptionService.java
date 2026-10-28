package org.cthub.backend.notification.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Registers, lists and removes the signed-in user's push devices. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushSubscriptionService {

    static final int MAX_ENDPOINT_LENGTH = 2048;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final PushSubscriptionRepository repository;
    private final UserRepository userRepository;
    private final PushProperties properties;

    /** A subscription as the browser reports it ({@code PushSubscription.toJSON()}). */
    public record Registration(String endpoint, String p256dh, String auth, boolean installed, String previousEndpoint,
                               boolean renewOnly) {
    }

    /**
     * Idempotent: re-registering a known endpoint refreshes it. An endpoint registered by another
     * user moves to this one (same browser, different account). {@code previousEndpoint} is removed;
     * browsers rotate subscriptions now and then. With {@code renewOnly}, only an existing registration
     * of this user is refreshed.
     */
    @Transactional
    public PushSubscription register(long userId, Registration registration, String userAgent) {
        validate(registration);

        if (registration.renewOnly()) {
            boolean known = repository.findByEndpoint(registration.endpoint())
                .filter(existing -> existing.getUser().getId().equals(userId))
                .isPresent();
            if (!known) {
                throw ApiException.notFound("PUSH_SUBSCRIPTION_NOT_FOUND", "This device is not registered for push.");
            }
        }

        if (registration.previousEndpoint() != null && !registration.previousEndpoint().equals(registration.endpoint())) {
            repository.deleteByEndpointAndUserId(registration.previousEndpoint(), userId);
        }

        Instant now = Instant.now();
        PushSubscription subscription = repository.findByEndpoint(registration.endpoint())
            .orElseGet(() -> PushSubscription.builder().endpoint(registration.endpoint()).createdAt(now).build());
        if (subscription.getUser() != null && !subscription.getUser().getId().equals(userId)) {
            log.info("Push subscription {} moves from user {} to user {}.", subscription.getId(), subscription.getUser().getId(), userId);
            subscription.setCreatedAt(now);
        }
        subscription.setUser(userRepository.getReferenceById(userId));
        subscription.setP256dh(registration.p256dh().strip());
        subscription.setAuth(registration.auth().strip());
        subscription.setUserAgent(truncate(userAgent));
        subscription.setInstalled(registration.installed());
        subscription.setLastSeenAt(now);
        PushSubscription saved = repository.save(subscription);

        enforceLimit(userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PushSubscription> list(long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void delete(long userId, long subscriptionId) {
        if (repository.deleteByIdAndUserId(subscriptionId, userId) == 0) {
            throw ApiException.notFound("PUSH_SUBSCRIPTION_NOT_FOUND", "Push subscription not found.");
        }
    }

    /** Removes this browser's subscription, e.g. on logout. Unknown endpoints are ignored. */
    @Transactional
    public void unregister(long userId, String endpoint) {
        repository.deleteByEndpointAndUserId(endpoint, userId);
    }

    private void enforceLimit(long userId) {
        List<PushSubscription> all = repository.findByUserIdOrderByCreatedAtDesc(userId);
        int max = Math.max(1, properties.getMaxSubscriptionsPerUser());
        if (all.size() <= max) {
            return;
        }
        List<PushSubscription> stale = all.stream()
            .sorted(Comparator.comparing(PushSubscription::getLastSeenAt).reversed())
            .skip(max)
            .toList();
        repository.deleteAll(stale);
    }

    private void validate(Registration registration) {
        if (!isAllowedEndpoint(registration.endpoint())) {
            throw invalid("The push endpoint is not served by a known push service.");
        }
        if (registration.previousEndpoint() != null && registration.previousEndpoint().length() > MAX_ENDPOINT_LENGTH) {
            throw invalid("The previous endpoint is too long.");
        }
        try {
            EcKeys.publicKey(EcKeys.decode(registration.p256dh()));
            if (EcKeys.decode(registration.auth()).length != 16) {
                throw invalid("The auth secret must be 16 bytes.");
            }
        } catch (IllegalArgumentException e) {
            throw invalid("The subscription keys are invalid.");
        }
    }

    boolean isAllowedEndpoint(String endpoint) {
        if (endpoint == null || endpoint.length() > MAX_ENDPOINT_LENGTH) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(endpoint.strip());
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
            || (uri.getPort() != -1 && uri.getPort() != 443)) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return properties.getAllowedEndpointHosts().stream()
            .map(pattern -> pattern.strip().toLowerCase(Locale.ROOT))
            .anyMatch(pattern -> pattern.startsWith("*.")
                ? host.endsWith(pattern.substring(1))
                : host.equals(pattern));
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("INVALID_PUSH_SUBSCRIPTION", message);
    }

    private static String truncate(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        return userAgent.length() <= MAX_USER_AGENT_LENGTH ? userAgent : userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
