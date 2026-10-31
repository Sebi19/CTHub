package org.cthub.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.auth.AccountService;
import org.cthub.backend.auth.credentials.TotpService;
import org.cthub.backend.auth.session.AuthSessionService;
import org.cthub.backend.auth.session.SessionIssuer;
import org.cthub.backend.auth.stepup.RequiresStepUp;
import org.cthub.backend.auth.webauthn.WebAuthnService;
import org.cthub.backend.dto.auth.AccountSecurityDto;
import org.cthub.backend.dto.auth.EmailChangeRequestDto;
import org.cthub.backend.dto.auth.LinkTokenRequestDto;
import org.cthub.backend.dto.auth.PasskeyDto;
import org.cthub.backend.dto.auth.PasskeyRegistrationOptionsDto;
import org.cthub.backend.dto.auth.PasskeyResponseDto;
import org.cthub.backend.dto.auth.RenamePasskeyRequestDto;
import org.cthub.backend.dto.auth.SessionDto;
import org.cthub.backend.dto.auth.SetPasswordRequestDto;
import org.cthub.backend.dto.auth.TotpCodeRequestDto;
import org.cthub.backend.dto.auth.TotpEnrollmentDto;
import org.cthub.backend.dto.auth.UserDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.mapper.AuthMapper;
import org.cthub.backend.mapper.UserMapper;
import org.cthub.backend.model.User;
import org.cthub.backend.service.RateLimiterService;
import org.cthub.backend.auth.RequestInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Account settings: optional password, authenticator app, passkeys, e-mail, sessions, deletion.
 * Everything that changes how the account can be entered needs a recent step-up.
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private final AccountService accountService;
    private final WebAuthnService webAuthnService;
    private final TotpService totpService;
    private final AuthSessionService sessionService;
    private final SessionIssuer sessionIssuer;
    private final RateLimiterService rateLimiter;
    private final AuthMapper authMapper;
    private final UserMapper userMapper;

    @GetMapping("/security")
    public AccountSecurityDto getAccountSecurity(@AuthenticationPrincipal User user) {
        return new AccountSecurityDto(user.hasPassword(), user.isTotpEnabled(), totpService.isAvailable(),
            authMapper.toPasskeyDtos(webAuthnService.list(user.getId())));
    }

    // --- password ---------------------------------------------------------------------------

    @PutMapping("/password")
    @RequiresStepUp
    public UserDto setPassword(@AuthenticationPrincipal User user, @Valid @RequestBody SetPasswordRequestDto body,
                               HttpServletRequest request) {
        return userMapper.toUserDto(accountService.setPassword(user, body.password(), request));
    }

    @DeleteMapping("/password")
    @RequiresStepUp
    public UserDto removePassword(@AuthenticationPrincipal User user, HttpServletRequest request) {
        return userMapper.toUserDto(accountService.removePassword(user, request));
    }

    // --- authenticator app ------------------------------------------------------------------

    @PostMapping("/totp")
    @RequiresStepUp
    public TotpEnrollmentDto beginTotp(@AuthenticationPrincipal User user) {
        TotpService.Enrollment enrollment = accountService.beginTotp(user);
        return new TotpEnrollmentDto(enrollment.secret(), enrollment.otpauthUri());
    }

    @PostMapping("/totp/confirm")
    @RequiresStepUp
    public UserDto confirmTotp(@AuthenticationPrincipal User user, @Valid @RequestBody TotpCodeRequestDto body,
                               HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("account:totp:confirm:" + user.getId(), 10, Duration.ofMinutes(10))) {
            throw ApiException.tooManyRequests("Too many codes tried. Please wait a few minutes.");
        }
        return userMapper.toUserDto(accountService.confirmTotp(user, body.code(), request));
    }

    @DeleteMapping("/totp")
    @RequiresStepUp
    public UserDto disableTotp(@AuthenticationPrincipal User user, HttpServletRequest request) {
        return userMapper.toUserDto(accountService.disableTotp(user, request));
    }

    // --- passkeys ---------------------------------------------------------------------------

    @GetMapping("/passkeys")
    public List<PasskeyDto> listPasskeys(@AuthenticationPrincipal User user) {
        return authMapper.toPasskeyDtos(webAuthnService.list(user.getId()));
    }

    /** A fresh sign-in counts as stepped up, so the post-login "add a passkey" nudge needs no extra prompt. */
    @PostMapping("/passkeys/options")
    @RequiresStepUp
    public PasskeyRegistrationOptionsDto passkeyRegistrationOptions(@AuthenticationPrincipal User user,
                                                                    HttpServletRequest request) {
        return webAuthnService.registrationOptions(user, request.getSession(true));
    }

    @PostMapping("/passkeys")
    @RequiresStepUp
    public PasskeyDto registerPasskey(@AuthenticationPrincipal User user, @Valid @RequestBody PasskeyResponseDto body,
                                      HttpServletRequest request) {
        return authMapper.toPasskeyDto(accountService.registerPasskey(user, body.credentialJson(), request));
    }

    @PatchMapping("/passkeys/{id}")
    public PasskeyDto renamePasskey(@AuthenticationPrincipal User user, @PathVariable long id,
                                    @Valid @RequestBody RenamePasskeyRequestDto body) {
        return authMapper.toPasskeyDto(webAuthnService.rename(user.getId(), id, body.name().strip()));
    }

    @DeleteMapping("/passkeys/{id}")
    @RequiresStepUp
    public ResponseEntity<Void> deletePasskey(@AuthenticationPrincipal User user, @PathVariable long id,
                                              HttpServletRequest request) {
        accountService.deletePasskey(user, id, request);
        return ResponseEntity.noContent().build();
    }

    // --- e-mail -----------------------------------------------------------------------------

    @PostMapping("/email")
    @RequiresStepUp
    public ResponseEntity<Void> requestEmailChange(@AuthenticationPrincipal User user,
                                                   @Valid @RequestBody EmailChangeRequestDto body,
                                                   HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("account:email-change:" + user.getId(), 5, Duration.ofHours(1))) {
            throw ApiException.tooManyRequests("Too many requests. Please try again later.");
        }
        accountService.requestEmailChange(user, body.newEmail(), request);
        return ResponseEntity.accepted().build();
    }

    /** Possession of the new mailbox is the proof, so this works signed out too. */
    @PostMapping("/email/confirm")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> confirmEmailChange(@AuthenticationPrincipal User principal,
                                                   @Valid @RequestBody LinkTokenRequestDto body,
                                                   HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("account:email-confirm:ip:" + RequestInfo.clientIp(request), 20, Duration.ofHours(1))) {
            throw ApiException.tooManyRequests("Too many requests. Please try again later.");
        }
        User updated = accountService.confirmEmailChange(body.token(), request);
        if (principal != null && principal.getId().equals(updated.getId())) {
            sessionIssuer.refreshPrincipal(updated, request);
        }
        return ResponseEntity.noContent().build();
    }

    // --- sessions ---------------------------------------------------------------------------

    @GetMapping("/sessions")
    public List<SessionDto> listSessions(@AuthenticationPrincipal User user, HttpServletRequest request) {
        UUID current = SessionIssuer.currentSessionId(request).orElse(null);
        return sessionService.listLive(user.getId()).stream()
            .map(session -> authMapper.toSessionDto(session, current))
            .toList();
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                              HttpServletRequest request, HttpServletResponse response) {
        if (!sessionService.revokeOwn(user.getId(), id)) {
            throw ApiException.notFound("SESSION_NOT_FOUND", "Session not found.");
        }
        if (id.equals(SessionIssuer.currentSessionId(request).orElse(null))) {
            sessionIssuer.clear(request, response);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/revoke-others")
    public ResponseEntity<Void> revokeOtherSessions(@AuthenticationPrincipal User user, HttpServletRequest request) {
        accountService.signOutOtherSessions(user, request);
        return ResponseEntity.noContent().build();
    }

    // --- deletion ---------------------------------------------------------------------------

    @DeleteMapping
    @RequiresStepUp
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user, HttpServletRequest request,
                                              HttpServletResponse response) {
        accountService.deleteAccount(user, request, response);
        return ResponseEntity.noContent().build();
    }
}
