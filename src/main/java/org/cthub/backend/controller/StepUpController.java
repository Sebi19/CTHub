package org.cthub.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.auth.AuthCookies;
import org.cthub.backend.auth.RequestInfo;
import org.cthub.backend.auth.attempt.LoginAttemptService;
import org.cthub.backend.auth.credentials.TotpService;
import org.cthub.backend.auth.session.AuthSessionService;
import org.cthub.backend.auth.session.SessionIssuer;
import org.cthub.backend.auth.webauthn.WebAuthnService;
import org.cthub.backend.dto.auth.LoginAttemptStateDto;
import org.cthub.backend.dto.auth.PasskeyAuthenticationOptionsDto;
import org.cthub.backend.dto.auth.PasskeyResponseDto;
import org.cthub.backend.dto.auth.PushEndpointRequestDto;
import org.cthub.backend.dto.auth.StepUpMethodsDto;
import org.cthub.backend.dto.auth.TotpCodeRequestDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.mapper.AuthMapper;
import org.cthub.backend.model.User;
import org.cthub.backend.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Re-proving identity before sensitive account changes ({@code @RequiresStepUp}): a passkey, an
 * authenticator code, or the push / e-mail challenge (completed via
 * {@code POST /api/auth/attempts/current/complete}). A password alone is not enough.
 */
@RestController
@RequestMapping("/api/auth/step-up")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class StepUpController {

    private final WebAuthnService webAuthnService;
    private final TotpService totpService;
    private final AuthSessionService sessionService;
    private final LoginAttemptService attemptService;
    private final AuthCookies cookies;
    private final RateLimiterService rateLimiter;
    private final AuthMapper authMapper;
    private final Clock clock;

    @GetMapping("/methods")
    public StepUpMethodsDto stepUpMethods(@AuthenticationPrincipal User user) {
        return new StepUpMethodsDto(webAuthnService.hasPasskeys(user.getId()),
            user.isTotpEnabled() && totpService.isAvailable());
    }

    @PostMapping("/totp")
    public ResponseEntity<Void> stepUpWithTotp(@AuthenticationPrincipal User user, @Valid @RequestBody TotpCodeRequestDto body,
                                     HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("auth:totp:user:" + user.getId(), 5, Duration.ofMinutes(5))) {
            throw ApiException.tooManyRequests("Too many codes tried. Please wait a few minutes.");
        }
        if (!totpService.verify(user, body.code())) {
            throw ApiException.badRequest("TOTP_INVALID", "That code is not valid.");
        }
        sessionService.markSteppedUp(currentSession(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/passkey/options")
    public PasskeyAuthenticationOptionsDto stepUpPasskeyOptions(@AuthenticationPrincipal User user, HttpServletRequest request) {
        return webAuthnService.authenticationOptions(request.getSession(true), user);
    }

    @PostMapping("/passkey")
    public ResponseEntity<Void> stepUpWithPasskey(@AuthenticationPrincipal User user, @Valid @RequestBody PasskeyResponseDto body,
                                        HttpServletRequest request) {
        webAuthnService.authenticate(body.credentialJson(), request.getSession(true), user);
        sessionService.markSteppedUp(currentSession(request));
        return ResponseEntity.noContent().build();
    }

    /** Starts a push / e-mail challenge for this session; the browser then polls like a sign-in. */
    @PostMapping("/challenge")
    public LoginAttemptStateDto startStepUpChallenge(@AuthenticationPrincipal User user, @Valid @RequestBody PushEndpointRequestDto body,
                                          HttpServletRequest request, HttpServletResponse response) {
        if (!rateLimiter.tryAcquire("auth:stepup:user:" + user.getId(), 10, Duration.ofHours(1))) {
            throw ApiException.tooManyRequests("Too many verification requests. Please try again later.");
        }
        LoginAttemptService.Started started = attemptService.startStepUp(user, currentSession(request),
            RequestInfo.locale(null, request), RequestInfo.userAgent(request), RequestInfo.clientIp(request),
            body.pushEndpoint(), cookies.readAttempt(request));
        cookies.writeAttempt(response, started.nonce(), LoginAttemptService.ATTEMPT_TTL);
        return authMapper.toStateDto(started.attempt(), clock.instant());
    }

    private static UUID currentSession(HttpServletRequest request) {
        return SessionIssuer.currentSessionId(request)
            .orElseThrow(() -> ApiException.unauthorized("NOT_AUTHENTICATED", "Not authenticated"));
    }
}
