package org.cthub.backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.dto.auth.ClaimRegistrationRequestDto;
import org.cthub.backend.dto.auth.CompleteProfileRequestDto;
import org.cthub.backend.dto.auth.ConfirmChallengeRequestDto;
import org.cthub.backend.dto.auth.EmailLinkRequestDto;
import org.cthub.backend.dto.auth.LoginRequestDto;
import org.cthub.backend.dto.auth.RegisterRequestDto;
import org.cthub.backend.dto.auth.RegistrationResponseDto;
import org.cthub.backend.dto.auth.RegistrationStartResponseDto;
import org.cthub.backend.dto.auth.RegistrationStatusResponseDto;
import org.cthub.backend.dto.auth.ResendVerificationRequestDto;
import org.cthub.backend.dto.auth.ResetPasswordRequestDto;
import org.cthub.backend.dto.auth.UserDto;
import org.cthub.backend.dto.auth.VerificationChallengeResponseDto;
import org.cthub.backend.dto.auth.VerifyEmailRequestDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.mapper.UserMapper;
import org.cthub.backend.model.User;
import org.cthub.backend.repository.UserRepository;
import org.cthub.backend.service.RateLimiterService;
import org.cthub.backend.service.RegistrationService;
import org.cthub.backend.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.cthub.backend.notification.live.LiveNotificationBroker;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private static final String GENERIC_LINK_MESSAGE =
        "If this e-mail address belongs to an account, you will receive further instructions shortly.";
    private static final String INVALID_TOKEN_MESSAGE =
        "This link is invalid, expired, or has already been used.";
    /** Identifies the browser that started a registration, so it can skip its own confirmation prompt. */
    private static final String REGISTRATION_COOKIE = "ct_registration";
    private static final String REJECTED_PREFIX = "registration-rejected:";
    private static final Duration REJECTION_COOLDOWN = Duration.ofHours(1);

    private final AuthenticationManager authenticationManager;
    private final RememberMeServices rememberMeServices;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RegistrationService registrationService;
    private final RateLimiterService rateLimiterService;
    private final LiveNotificationBroker liveNotificationBroker;

    // POST /api/auth/login
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public UserDto login(
        @Valid @RequestBody LoginRequestDto loginRequest,
        HttpServletRequest request,
        HttpServletResponse response) {

        String cleanEmail = normalizedEmail(loginRequest.getEmail());

        // Passwordless accounts would otherwise fail with a misleading "bad credentials"; point
        // them at the login-link flow instead.
        userRepository.findByEmail(cleanEmail).ifPresent(existing -> {
            if (existing.getEmailVerifiedAt() != null && !existing.hasPassword()) {
                throw ApiException.forbidden(
                    "NO_PASSWORD_SET",
                    "This account signs in with an e-mail link. Request one to continue.");
            }
        });

        Authentication authenticationResponse;
        try {
            authenticationResponse = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(cleanEmail, loginRequest.getPassword()));
        } catch (DisabledException e) {
            throw ApiException.forbidden(
                "ACCOUNT_NOT_VERIFIED",
                "Please verify your e-mail address before logging in.");
        }

        if (!(authenticationResponse.getPrincipal() instanceof User user)) {
            throw ApiException.unauthorized("AUTHENTICATION_FAILED", "Could not sign you in.");
        }

        establishSession(authenticationResponse, request, response);
        return userMapper.toUserDto(user);
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            liveNotificationBroker.closeSession(session.getId());
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        response.addCookie(expiredCookie("remember-me"));
        response.addCookie(expiredCookie("JSESSIONID"));
        return ResponseEntity.ok("Logged out successfully");
    }

    // GET /api/auth/me (Check current session)
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        return currentUser()
            .map(user -> ResponseEntity.ok(userMapper.toUserDto(user)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // POST /api/auth/register
    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public RegistrationStartResponseDto register(
        @Valid @RequestBody RegisterRequestDto registerRequest,
        HttpServletRequest request,
        HttpServletResponse response) {

        requireQuota("register", registerRequest.getEmail(), request);

        if (rateLimiterService.isBlocked(REJECTED_PREFIX + normalizedEmail(registerRequest.getEmail()))) {
            // Indistinguishable from an ordinary rate limit on purpose: confirming that an address
            // had been rejected would leak that somebody tried to register it.
            throw ApiException.tooManyRequests("Too many requests. Please try again later.");
        }

        RegistrationService.RegistrationStart start = registrationService.register(
            registerRequest.getEmail(),
            localeOf(registerRequest.getLocale(), request));

        if (start.registrationId() != null) {
            // Lets the verification page recognise its own browser and skip the confirmation prompt.
            // A cookie, not sessionStorage: an e-mailed link opens a fresh tab, which inherits
            // cookies but not sessionStorage, so the originating browser would challenge itself.
            response.addCookie(registrationCookie(start.registrationId()));
        }

        return RegistrationStartResponseDto.builder()
            .status(start.status() == RegistrationService.RegistrationStart.Status.ALREADY_REGISTERED
                ? RegistrationStartResponseDto.RegistrationStartStatus.ALREADY_REGISTERED
                : RegistrationStartResponseDto.RegistrationStartStatus.PENDING)
            .registrationId(start.registrationId())
            .challengeNumber(start.challengeNumber())
            .build();
    }

    // GET /api/auth/registration-status
    @GetMapping("/registration-status")
    @PreAuthorize("permitAll()")
    public RegistrationStatusResponseDto registrationStatus(@RequestParam String registrationId) {
        RegistrationService.RegistrationState state = registrationService.registrationState(registrationId);
        return RegistrationStatusResponseDto.builder()
            .status(RegistrationStatusResponseDto.RegistrationStatus.valueOf(state.name()))
            .build();
    }

    // GET /api/auth/verification-challenge
    @GetMapping("/verification-challenge")
    @PreAuthorize("permitAll()")
    public VerificationChallengeResponseDto verificationChallenge(
        @RequestParam String token,
        HttpServletRequest request) {

        RegistrationService.ChallengeView view =
            registrationService.openChallenge(token, readRegistrationCookie(request));

        return switch (view) {
            case RegistrationService.ChallengeView.Options options -> VerificationChallengeResponseDto.builder()
                .kind(VerificationChallengeResponseDto.ChallengeKind.CHALLENGE)
                .numbers(options.numbers())
                .email(options.email())
                .build();
            case RegistrationService.ChallengeView.SameBrowser ignored -> VerificationChallengeResponseDto.builder()
                .kind(VerificationChallengeResponseDto.ChallengeKind.SAME_BROWSER)
                .build();
            case RegistrationService.ChallengeView.None ignored -> VerificationChallengeResponseDto.builder()
                .kind(VerificationChallengeResponseDto.ChallengeKind.NO_CHALLENGE)
                .build();
            case RegistrationService.ChallengeView.Invalid ignored -> VerificationChallengeResponseDto.builder()
                .kind(VerificationChallengeResponseDto.ChallengeKind.INVALID)
                .build();
        };
    }

    // POST /api/auth/verification-confirm
    @PostMapping("/verification-confirm")
    @PreAuthorize("permitAll()")
    public RegistrationResponseDto confirmChallenge(
        @Valid @RequestBody ConfirmChallengeRequestDto confirmRequest,
        HttpServletRequest request) {

        // Guessing is bounded by burning a link per wrong pick, but rate limit it anyway so the
        // three options cannot be ground down by automation.
        requireQuota("confirm", confirmRequest.getToken(), request);

        boolean correct;
        try {
            correct = registrationService.confirmChallenge(confirmRequest.getToken(), confirmRequest.getNumber());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("TOKEN_INVALID", INVALID_TOKEN_MESSAGE);
        }

        if (!correct) {
            throw ApiException.badRequest("CONFIRMATION_MISMATCH",
                "That number does not match. For your safety this link has been deactivated.");
        }

        // Deliberately no session for this device: a correct pick hands control back to the browser
        // that started the registration, which claims it via /registration-claim.
        return RegistrationResponseDto.builder()
            .message("Confirmed. Continue in the window where you started the registration.")
            .build();
    }

    // POST /api/auth/verification-reject
    @PostMapping("/verification-reject")
    @PreAuthorize("permitAll()")
    public RegistrationResponseDto rejectRegistration(
        @Valid @RequestBody VerifyEmailRequestDto rejectRequest) {

        // A mailbox owner saying "that wasn't me" should not be able to be nagged again straight
        // away, so the address goes on a short cooling-off period before it can be registered again.
        registrationService.rejectRegistration(rejectRequest.getToken())
            .ifPresent(email -> rateLimiterService.block(
                REJECTED_PREFIX + normalizedEmail(email), REJECTION_COOLDOWN));

        // Always generic: whether the link was still live is not the rejecting party's business.
        return RegistrationResponseDto.builder()
            .message("Thanks - the registration has been cancelled.")
            .build();
    }

    // POST /api/auth/registration-claim
    @PostMapping("/registration-claim")
    @PreAuthorize("permitAll()")
    public UserDto claimRegistration(
        @Valid @RequestBody ClaimRegistrationRequestDto claimRequest,
        HttpServletRequest request,
        HttpServletResponse response) {

        User user = registrationService.claimRegistration(claimRequest.getRegistrationId())
            .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", INVALID_TOKEN_MESSAGE));

        response.addCookie(expiredCookie(REGISTRATION_COOKIE));
        return signInAndMap(user, request, response);
    }

    // POST /api/auth/verify-email
    @PostMapping("/verify-email")
    @PreAuthorize("permitAll()")
    public UserDto verifyEmail(
        @Valid @RequestBody VerifyEmailRequestDto verifyRequest,
        HttpServletRequest request,
        HttpServletResponse response) {

        User user = registrationService.verifyEmail(verifyRequest.getToken())
            .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", INVALID_TOKEN_MESSAGE));

        // Clicking the link proves inbox ownership, so sign this browser in and send it straight to
        // the complete-profile step instead of asking for credentials it may not even have yet.
        response.addCookie(expiredCookie(REGISTRATION_COOKIE));
        return signInAndMap(user, request, response);
    }

    // POST /api/auth/resend-verification
    @PostMapping("/resend-verification")
    @PreAuthorize("permitAll()")
    public RegistrationResponseDto resendVerification(
        @Valid @RequestBody ResendVerificationRequestDto resendRequest,
        HttpServletRequest request) {

        requireQuota("resend", resendRequest.getEmail(), request);
        registrationService.resendVerification(
            resendRequest.getEmail(),
            localeOf(resendRequest.getLocale(), request));

        return RegistrationResponseDto.builder().message(GENERIC_LINK_MESSAGE).build();
    }

    // POST /api/auth/complete-profile
    @PostMapping("/complete-profile")
    public UserDto completeProfile(@Valid @RequestBody CompleteProfileRequestDto completeRequest) {
        User user = currentUser()
            .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "You must be signed in."));

        User updated = registrationService.completeProfile(
            user,
            completeRequest.getFirstName(),
            completeRequest.getLastName(),
            completeRequest.getPassword());

        // Refresh the principal so the gate and /me immediately see the completed profile.
        refreshPrincipal(updated);
        return userMapper.toUserDto(updated);
    }

    // POST /api/auth/forgot-password
    @PostMapping("/forgot-password")
    @PreAuthorize("permitAll()")
    public RegistrationResponseDto forgotPassword(
        @Valid @RequestBody EmailLinkRequestDto forgotRequest,
        HttpServletRequest request) {

        requireQuota("forgot-password", forgotRequest.getEmail(), request);
        registrationService.requestPasswordReset(
            forgotRequest.getEmail(),
            localeOf(forgotRequest.getLocale(), request));

        return RegistrationResponseDto.builder().message(GENERIC_LINK_MESSAGE).build();
    }

    // POST /api/auth/reset-password
    @PostMapping("/reset-password")
    @PreAuthorize("permitAll()")
    public UserDto resetPassword(
        @Valid @RequestBody ResetPasswordRequestDto resetRequest,
        HttpServletRequest request,
        HttpServletResponse response) {

        User user = registrationService.resetPassword(resetRequest.getToken(), resetRequest.getPassword())
            .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", INVALID_TOKEN_MESSAGE));

        return signInAndMap(user, request, response);
    }

    // POST /api/auth/login-link
    @PostMapping("/login-link")
    @PreAuthorize("permitAll()")
    public RegistrationResponseDto requestLoginLink(
        @Valid @RequestBody EmailLinkRequestDto linkRequest,
        HttpServletRequest request) {

        requireQuota("login-link", linkRequest.getEmail(), request);
        registrationService.requestLoginLink(
            linkRequest.getEmail(),
            localeOf(linkRequest.getLocale(), request));

        return RegistrationResponseDto.builder().message(GENERIC_LINK_MESSAGE).build();
    }

    // POST /api/auth/login-link/consume
    @PostMapping("/login-link/consume")
    @PreAuthorize("permitAll()")
    public UserDto consumeLoginLink(
        @Valid @RequestBody VerifyEmailRequestDto consumeRequest,
        HttpServletRequest request,
        HttpServletResponse response) {

        User user = registrationService.consumeLoginLink(consumeRequest.getToken())
            .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", INVALID_TOKEN_MESSAGE));

        return signInAndMap(user, request, response);
    }

    /** Signs a user in without a password, for flows where a mailed token already proved ownership. */
    private UserDto signInAndMap(User user, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication =
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        establishSession(authentication, request, response);
        return userMapper.toUserDto(user);
    }

    private void establishSession(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        rememberMeServices.loginSuccess(request, response, authentication);
    }

    /** Swaps the stale principal in the current context for a freshly loaded one. */
    private void refreshPrincipal(User user) {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication current = context.getAuthentication();
        if (current == null) {
            return;
        }
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    private Optional<User> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    private void requireQuota(String action, String email, HttpServletRequest request) {
        boolean allowed = rateLimiterService.tryAcquire(action + ":ip:" + clientIp(request), 10, Duration.ofHours(1))
            && rateLimiterService.tryAcquire(action + ":email:" + normalizedEmail(email), 3, Duration.ofHours(1));
        if (!allowed) {
            throw ApiException.tooManyRequests("Too many requests. Please try again later.");
        }
    }

    /**
     * Prefers the language the user explicitly selected in the UI over {@code Accept-Language},
     * which reflects browser configuration rather than an actual choice.
     */
    private Locale localeOf(String requestedLocale, HttpServletRequest request) {
        if (requestedLocale != null && !requestedLocale.isBlank()) {
            Locale parsed = Locale.forLanguageTag(requestedLocale);
            if (!parsed.getLanguage().isEmpty()) {
                return parsed;
            }
        }
        Locale locale = request.getLocale();
        return locale != null ? locale : Locale.GERMAN;
    }

    private Cookie expiredCookie(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

    /**
     * Marks this browser as the one that started a registration. Lax rather than Strict, because the
     * verification page is typically reached by following a link out of a mail client.
     */
    private Cookie registrationCookie(String registrationId) {
        Cookie cookie = new Cookie(REGISTRATION_COOKIE, registrationId);
        cookie.setMaxAge((int) TokenService.REGISTRATION_STATUS_TTL.toSeconds());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    private String readRegistrationCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
            .filter(cookie -> REGISTRATION_COOKIE.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    private String normalizedEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
