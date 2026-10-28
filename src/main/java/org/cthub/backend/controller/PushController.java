package org.cthub.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.notification.PushConfigDto;
import org.cthub.backend.dto.notification.PushSubscriptionDto;
import org.cthub.backend.dto.notification.PushSubscriptionRequestDto;
import org.cthub.backend.dto.notification.PushTestResultDto;
import org.cthub.backend.dto.notification.PushUnregisterRequestDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.mapper.PushSubscriptionMapper;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.push.PushDispatcher;
import org.cthub.backend.notification.push.PushSubscriptionService;
import org.cthub.backend.notification.push.VapidAuth;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/** The signed-in user's Web Push devices. Which notification types get pushed is a preference. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/notifications/push")
public class PushController {

    private final VapidAuth vapid;
    private final PushSubscriptionService subscriptionService;
    private final PushSubscriptionMapper mapper;
    private final PushDispatcher dispatcher;

    // GET /api/notifications/push/config
    @GetMapping("/config")
    public PushConfigDto getPushConfig() {
        return new PushConfigDto(vapid.isEnabled(), vapid.publicKey());
    }

    /** Idempotent; the app calls it on every start to keep the subscription fresh. */
    // PUT /api/notifications/push/subscriptions
    @PutMapping("/subscriptions")
    public PushSubscriptionDto registerPushSubscription(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody PushSubscriptionRequestDto request,
        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        requireEnabled();
        return mapper.toDto(subscriptionService.register(user.getId(), mapper.toRegistration(request), userAgent));
    }

    // GET /api/notifications/push/subscriptions
    @GetMapping("/subscriptions")
    public List<PushSubscriptionDto> listPushSubscriptions(@AuthenticationPrincipal User user) {
        return mapper.toDtos(subscriptionService.list(user.getId()));
    }

    // DELETE /api/notifications/push/subscriptions/{id}
    @DeleteMapping("/subscriptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePushSubscription(@AuthenticationPrincipal User user, @PathVariable long id) {
        subscriptionService.delete(user.getId(), id);
    }

    /** Removes this browser's subscription by endpoint (it doesn't know its id after a reload). */
    // POST /api/notifications/push/subscriptions/unregister
    @PostMapping("/subscriptions/unregister")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterPushSubscription(
        @AuthenticationPrincipal User user, @Valid @RequestBody PushUnregisterRequestDto request) {
        subscriptionService.unregister(user.getId(), request.getEndpoint());
    }

    // POST /api/notifications/push/test
    @PostMapping("/test")
    public PushTestResultDto sendTestPush(@AuthenticationPrincipal User user) {
        requireEnabled();
        Locale locale = user.getPreferredLocale() != null ? Locale.forLanguageTag(user.getPreferredLocale()) : Locale.GERMAN;
        PushDispatcher.TestResult result = dispatcher.sendTest(user.getId(), locale);
        return new PushTestResultDto(result.devices(), result.delivered());
    }

    private void requireEnabled() {
        if (!vapid.isEnabled()) {
            throw ApiException.badRequest("PUSH_DISABLED", "Push notifications are not configured on this server.");
        }
    }
}
