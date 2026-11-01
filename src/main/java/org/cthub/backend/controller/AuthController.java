package org.cthub.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.auth.AccountService;
import org.cthub.backend.auth.AuthCookies;
import org.cthub.backend.auth.RequestInfo;
import org.cthub.backend.auth.attempt.LoginAttemptService;
import org.cthub.backend.auth.credentials.TotpService;
import org.cthub.backend.auth.session.AuthMethod;
import org.cthub.backend.auth.session.AuthSessionService;
import org.cthub.backend.auth.session.SessionIssuer;
import org.cthub.backend.auth.session.SessionRevokeReason;
import org.cthub.backend.auth.webauthn.WebAuthnService;
import org.cthub.backend.dto.auth.AuthNextStep;
import org.cthub.backend.dto.auth.CompleteProfileRequestDto;
import org.cthub.backend.dto.auth.IdentifyRequestDto;
import org.cthub.backend.dto.auth.IdentifyResponseDto;
import org.cthub.backend.dto.auth.LoginAttemptStateDto;
import org.cthub.backend.dto.auth.LoginResultDto;
import org.cthub.backend.dto.auth.LoginResultStatus;
import org.cthub.backend.dto.auth.PasskeyAuthenticationOptionsDto;
import org.cthub.backend.dto.auth.PasskeyResponseDto;
import org.cthub.backend.dto.auth.PasswordLoginRequestDto;
import org.cthub.backend.dto.auth.StartAttemptRequestDto;
import org.cthub.backend.dto.auth.TotpCodeRequestDto;
import org.cthub.backend.dto.auth.UserDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.mapper.AuthMapper;
import org.cthub.backend.mapper.UserMapper;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.push.PushSubscriptionService;
import org.cthub.backend.repository.UserRepository;
import org.cthub.backend.service.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

/**
 * The identifier-first sign-in (which is also sign-up): one e-mail field, then passkey, password
 * (+ TOTP) or the number-matching challenge. See {@link LoginAttemptController} for the challenge
 * itself. Nothing here reveals whether an address has an account unless it opted into a passkey or
 * a password.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AuthController {

    private static final String PENDING_TOTP = "AUTH_PENDING_TOTP";
    private static final Duration PENDING_TOTP_TTL = Duration.ofMinutes(5);

    private final LoginAttemptService attemptService;
    private final WebAuthnService webAuthnService;
    private final TotpService totpService;
    private final AccountService accountService;
    private final PushSubscriptionService pushSubscriptionService;
    private final SessionIssuer sessionIssuer;
    private final AuthSessionService sessionService;
    private final AuthCookies cookies;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiter;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final Clock clock;

    /** Compared against when the account has no password, so timing does not tell the cases apart. */
    private volatile String dummyHash;

    private record PendingTotp(long userId, Instant expiresAt) implements Serializable {
    }

    @GetMapping("/me")
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User user) {
        return user == null
            ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            : ResponseEntity.ok(userMapper.toUserDto(user));
    }

    @PostMapping("/identify")
    @PreAuthorize("permitAll()")
    public IdentifyResponseDto identify(@Valid @RequestBody IdentifyRequestDto body, HttpServletRequest request,
                                        HttpServletResponse response) {
        String email = RequestInfo.normalizeEmail(body.email());
        limitStart(email, request);
        Optional<User> user = userRepository.findByEmail(email);
        boolean passwordAvailable = user.map(User::hasPassword).orElse(false);
        if (user.isPresent() && webAuthnService.hasPasskeys(user.get().getId())) {
            return new IdentifyResponseDto(AuthNextStep.PASSKEY, passwordAvailable, null);
        }
        if (passwordAvailable) {
            return new IdentifyResponseDto(AuthNextStep.PASSWORD, true, null);
        }
        return new IdentifyResponseDto(AuthNextStep.CHALLENGE, false,
            startChallenge(email, body.pushEndpoint(), body.locale(), request, response));
    }

    /** "Use push / e-mail link instead" from the passkey or password step. */
    @PostMapping("/attempts")
    @PreAuthorize("permitAll()")
    public LoginAttemptStateDto startAttempt(@Valid @RequestBody StartAttemptRequestDto body,
                                             HttpServletRequest request, HttpServletResponse response) {
        String email = RequestInfo.normalizeEmail(body.email());
        limitStart(email, request);
        return startChallenge(email, body.pushEndpoint(), body.locale(), request, response);
    }

    private LoginAttemptStateDto startChallenge(String email, String pushEndpoint, String locale,
                                                HttpServletRequest request, HttpServletResponse response) {
        // A signed-out browser cannot own a live subscription; whatever is left of it is an orphan.
        if (pushEndpoint != null && !pushEndpoint.isBlank() && SessionIssuer.currentSessionId(request).isEmpty()) {
            pushSubscriptionService.removeOrphan(pushEndpoint.strip());
        }
        LoginAttemptService.Started started = attemptService.startLogin(email, RequestInfo.locale(locale, request),
            RequestInfo.userAgent(request), RequestInfo.clientIp(request), pushEndpoint, cookies.readAttempt(request));
        cookies.writeAttempt(response, started.nonce(), LoginAttemptService.ATTEMPT_TTL);
        return authMapper.toStateDto(started.attempt(), clock.instant());
    }

    // --- password (+ TOTP) ------------------------------------------------------------------

    @PostMapping("/login/password")
    @PreAuthorize("permitAll()")
    public LoginResultDto loginWithPassword(@Valid @RequestBody PasswordLoginRequestDto body,
                                            HttpServletRequest request, HttpServletResponse response) {
        String email = RequestInfo.normalizeEmail(body.email());
        if (!rateLimiter.tryAcquire("auth:pw:ip:" + RequestInfo.clientIp(request), 50, Duration.ofHours(1))
            || !rateLimiter.tryAcquire("auth:pw:email:" + email, 10, Duration.ofMinutes(15))) {
            throw ApiException.tooManyRequests("Too many sign-in attempts. Please try again later.");
        }
        Optional<User> found = userRepository.findByEmail(email).filter(User::hasPassword);
        String hash = found.map(User::getPassword).orElseGet(this::dummyHash);
        boolean matches = passwordEncoder.matches(body.password(), hash);
        if (found.isEmpty() || !matches) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "E-mail or password is incorrect.");
        }
        User user = found.get();
        requireActive(user);
        if (user.isTotpEnabled()) {
            if (!totpService.isAvailable()) {
                // Never silently drop the second factor because of a configuration problem.
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TOTP_UNAVAILABLE",
                    "Two-factor sign-in is temporarily unavailable. Use a passkey or the e-mail link.");
            }
            HttpSession httpSession = request.getSession(true);
            httpSession.setAttribute(PENDING_TOTP, new PendingTotp(user.getId(), clock.instant().plus(PENDING_TOTP_TTL)));
            return new LoginResultDto(LoginResultStatus.TOTP_REQUIRED, null);
        }
        sessionIssuer.signIn(user, EnumSet.of(AuthMethod.PASSWORD), request, response);
        return new LoginResultDto(LoginResultStatus.SIGNED_IN, userMapper.toUserDto(user));
    }

    /** Second step after a correct password. There is deliberately no TOTP-only sign-in. */
    @PostMapping("/login/password/totp")
    @PreAuthorize("permitAll()")
    public LoginResultDto loginWithPasswordAndTotp(@Valid @RequestBody TotpCodeRequestDto body, HttpServletRequest request,
                                    HttpServletResponse response) {
        HttpSession httpSession = request.getSession(false);
        Object pending = httpSession == null ? null : httpSession.getAttribute(PENDING_TOTP);
        if (!(pending instanceof PendingTotp(long userId, Instant expiresAt)) || !expiresAt.isAfter(clock.instant())) {
            throw ApiException.unauthorized("TOTP_SESSION_EXPIRED", "Please sign in with your password again.");
        }
        if (!rateLimiter.tryAcquire("auth:totp:user:" + userId, 5, Duration.ofMinutes(5))) {
            httpSession.removeAttribute(PENDING_TOTP);
            throw ApiException.tooManyRequests("Too many codes tried. Please sign in again later.");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("TOTP_SESSION_EXPIRED", "Please sign in again."));
        if (!totpService.verify(user, body.code())) {
            throw ApiException.unauthorized("TOTP_INVALID", "That code is not valid.");
        }
        requireActive(user);
        httpSession.removeAttribute(PENDING_TOTP);
        sessionIssuer.signIn(user, EnumSet.of(AuthMethod.PASSWORD, AuthMethod.TOTP), request, response);
        return new LoginResultDto(LoginResultStatus.SIGNED_IN, userMapper.toUserDto(user));
    }

    // --- passkeys ---------------------------------------------------------------------------

    /** Also used for autofill (conditional mediation) before any e-mail has been entered. */
    @PostMapping("/passkey/options")
    @PreAuthorize("permitAll()")
    public PasskeyAuthenticationOptionsDto passkeyLoginOptions(HttpServletRequest request) {
        limitPasskey(request);
        return webAuthnService.authenticationOptions(request.getSession(true), null);
    }

    @PostMapping("/passkey/login")
    @PreAuthorize("permitAll()")
    public UserDto loginWithPasskey(@Valid @RequestBody PasskeyResponseDto body, HttpServletRequest request,
                                    HttpServletResponse response) {
        limitPasskey(request);
        User user = webAuthnService.authenticate(body.credentialJson(), request.getSession(true), null);
        requireActive(user);
        sessionIssuer.signIn(user, EnumSet.of(AuthMethod.PASSKEY), request, response);
        return userMapper.toUserDto(user);
    }

    // --- session ----------------------------------------------------------------------------

    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionIssuer.signOut(request, response, SessionRevokeReason.LOGOUT);
        return ResponseEntity.noContent().build();
    }

    /** Called by the installed app (PWA) on start: its session slides for a year instead of 90 days. */
    @PostMapping("/session/installed-app")
    public ResponseEntity<Void> markInstalledApp(HttpServletRequest request) {
        SessionIssuer.currentSessionId(request).ifPresent(sessionService::markInstalledApp);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/complete-profile")
    public UserDto completeProfile(@AuthenticationPrincipal User user, @Valid @RequestBody CompleteProfileRequestDto body,
                                   HttpServletRequest request) {
        return userMapper.toUserDto(accountService.completeProfile(user, body.getFirstName(), body.getLastName(), request));
    }

    // --- helpers ----------------------------------------------------------------------------

    private void limitStart(String email, HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("auth:start:ip:" + RequestInfo.clientIp(request), 30, Duration.ofHours(1))
            || !rateLimiter.tryAcquire("auth:start:email:" + email, 10, Duration.ofHours(1))) {
            throw ApiException.tooManyRequests("Too many sign-in requests. Please try again later.");
        }
    }

    private void limitPasskey(HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("auth:passkey:ip:" + RequestInfo.clientIp(request), 60, Duration.ofMinutes(15))) {
            throw ApiException.tooManyRequests("Too many sign-in attempts. Please try again later.");
        }
    }

    private static void requireActive(User user) {
        if (!user.isEnabled()) {
            throw ApiException.forbidden("ACCOUNT_DISABLED", "This account is disabled.");
        }
    }

    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            hash = passwordEncoder.encode("not-a-real-password-" + System.nanoTime());
            dummyHash = hash;
        }
        return hash;
    }
}
