package org.cthub.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.auth.credentials.PasswordPolicy;
import org.cthub.backend.auth.credentials.TotpService;
import org.cthub.backend.auth.session.AuthSessionRevokedEvent;
import org.cthub.backend.auth.session.AuthSessionService;
import org.cthub.backend.auth.session.SessionIssuer;
import org.cthub.backend.auth.session.SessionRevokeReason;
import org.cthub.backend.auth.webauthn.WebAuthnCredential;
import org.cthub.backend.auth.webauthn.WebAuthnService;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.render.EmailChangeModel;
import org.cthub.backend.mail.render.SecurityAlertModel;
import org.cthub.backend.mail.render.WelcomeModel;
import org.cthub.backend.model.User;
import org.cthub.backend.model.UserToken;
import org.cthub.backend.model.UserTokenType;
import org.cthub.backend.notification.NotificationRequest;
import org.cthub.backend.notification.NotificationService;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
import org.cthub.backend.service.TokenService;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Self-service account changes. Every credential change notifies the account's mailbox, so a
 * hijacked session cannot quietly entrench itself. The sensitive ones are gated by
 * {@code @RequiresStepUp} at the controller.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TotpService totpService;
    private final WebAuthnService webAuthnService;
    private final TokenService tokenService;
    private final AuthSessionService sessionService;
    private final SessionIssuer sessionIssuer;
    private final MailService mailService;
    private final MailProperties mailProperties;
    private final NotificationService notificationService;
    private final Clock clock;

    // --- profile ----------------------------------------------------------------------------

    @Transactional
    public User completeProfile(User principal, String firstName, String lastName, HttpServletRequest request) {
        User user = load(principal);
        boolean firstCompletion = !user.isProfileComplete();
        user.setFirstName(firstName.strip());
        user.setLastName(lastName.strip());
        if (user.getProfileCompletedAt() == null) {
            user.setProfileCompletedAt(clock.instant());
        }
        if (firstCompletion) {
            mailService.enqueue(MailTemplate.WELCOME, address(user), new WelcomeModel(user.getFirstName()),
                locale(user, request));
            notificationService.notify(NotificationRequest.builder()
                .type(NotificationType.WELCOME)
                .params(Map.of("firstName", user.getFirstName()))
                .dedupeKey("welcome")
                .build(), user.getId());
        }
        sessionIssuer.refreshPrincipal(user, request);
        return user;
    }

    // --- password ---------------------------------------------------------------------------

    @Transactional
    public User setPassword(User principal, String newPassword, HttpServletRequest request) {
        User user = load(principal);
        passwordPolicy.validate(newPassword);
        boolean hadPassword = user.hasPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        // Anyone who knew the old password and is signed in elsewhere keeps nothing from it.
        if (hadPassword) {
            sessionService.revokeAllExcept(user.getId(), SessionIssuer.currentSessionId(request).orElse(null),
                SessionRevokeReason.CREDENTIALS_CHANGED);
        }
        alert(user, hadPassword ? SecurityAlertModel.Kind.PASSWORD_CHANGED : SecurityAlertModel.Kind.PASSWORD_SET, request);
        sessionIssuer.refreshPrincipal(user, request);
        return user;
    }

    /** Back to passwordless. TOTP goes with it, since it is only ever a second factor to the password. */
    @Transactional
    public User removePassword(User principal, HttpServletRequest request) {
        User user = load(principal);
        if (!user.hasPassword()) {
            return user;
        }
        user.setPassword(null);
        if (user.isTotpEnabled()) {
            totpService.disable(user);
            alert(user, SecurityAlertModel.Kind.TOTP_DISABLED, request);
        }
        alert(user, SecurityAlertModel.Kind.PASSWORD_REMOVED, request);
        sessionIssuer.refreshPrincipal(user, request);
        return user;
    }

    // --- TOTP -------------------------------------------------------------------------------

    @Transactional
    public TotpService.Enrollment beginTotp(User principal) {
        User user = load(principal);
        if (!user.hasPassword()) {
            throw ApiException.badRequest("PASSWORD_REQUIRED",
                "Set a password first: the authenticator app is a second factor to it.");
        }
        if (user.isTotpEnabled()) {
            throw ApiException.badRequest("TOTP_ALREADY_ENABLED", "An authenticator app is already set up.");
        }
        return totpService.begin(user);
    }

    @Transactional
    public User confirmTotp(User principal, String code, HttpServletRequest request) {
        User user = load(principal);
        if (!totpService.confirm(user, code)) {
            throw ApiException.badRequest("TOTP_INVALID", "That code is not valid.");
        }
        alert(user, SecurityAlertModel.Kind.TOTP_ENABLED, request);
        return user;
    }

    @Transactional
    public User disableTotp(User principal, HttpServletRequest request) {
        User user = load(principal);
        if (user.isTotpEnabled()) {
            totpService.disable(user);
            alert(user, SecurityAlertModel.Kind.TOTP_DISABLED, request);
        }
        return user;
    }

    // --- passkeys ---------------------------------------------------------------------------

    @Transactional
    public WebAuthnCredential registerPasskey(User principal, String credentialJson, HttpServletRequest request) {
        User user = load(principal);
        WebAuthnCredential credential = webAuthnService.register(user, credentialJson,
            DeviceLabel.of(RequestInfo.userAgent(request)), request.getSession(true));
        alert(user, SecurityAlertModel.Kind.PASSKEY_ADDED, request);
        return credential;
    }

    @Transactional
    public void deletePasskey(User principal, long passkeyId, HttpServletRequest request) {
        User user = load(principal);
        webAuthnService.delete(user.getId(), passkeyId);
        alert(user, SecurityAlertModel.Kind.PASSKEY_REMOVED, request);
    }

    // --- e-mail change ----------------------------------------------------------------------

    /**
     * Sends a confirmation link to the new address; nothing changes until it is used. The response
     * is the same whether or not the address is taken, so this is no account-existence oracle.
     */
    @Transactional
    public void requestEmailChange(User principal, String newEmail, HttpServletRequest request) {
        User user = load(principal);
        String normalized = RequestInfo.normalizeEmail(newEmail);
        if (normalized.equals(user.getEmail())) {
            throw ApiException.badRequest("EMAIL_UNCHANGED", "That is already your e-mail address.");
        }
        if (userRepository.findByEmail(normalized).isPresent()) {
            log.info("E-mail change of user {} to a taken address ignored.", user.getId());
            return;
        }
        String token = tokenService.issue(user, UserTokenType.EMAIL_CHANGE, TokenService.EMAIL_CHANGE_TTL, normalized);
        String url = mailProperties.getBaseUrl() + "/account/confirm-email?token=" + token;
        mailService.enqueue(MailTemplate.EMAIL_CHANGE, new MailAddress(normalized, displayName(user)),
            new EmailChangeModel(user.getFirstName(), normalized, url, TokenService.EMAIL_CHANGE_TTL),
            locale(user, request));
    }

    /**
     * Applies a confirmed change. Possession of the new mailbox is the proof, so this works even
     * when signed out; the old address is told about it.
     */
    @Transactional
    public User confirmEmailChange(String rawToken, HttpServletRequest request) {
        UserToken token = tokenService.findUsable(rawToken, UserTokenType.EMAIL_CHANGE)
            .orElseThrow(() -> ApiException.badRequest("LINK_INVALID", "This link is invalid or has expired."));
        String newEmail = token.getPayload();
        User user = load(token.getUser());
        tokenService.consume(token);
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw ApiException.badRequest("EMAIL_TAKEN", "This e-mail address is already in use.");
        }
        MailAddress oldAddress = address(user);
        user.setEmail(newEmail);
        user.setEmailVerifiedAt(clock.instant());
        mailService.enqueue(MailTemplate.SECURITY_ALERT, oldAddress,
            new SecurityAlertModel(user.getFirstName(), SecurityAlertModel.Kind.EMAIL_CHANGED,
                DeviceLabel.of(RequestInfo.userAgent(request))),
            locale(user, request));
        userRepository.flush();
        return user;
    }

    // --- account deletion -------------------------------------------------------------------

    /** Every dependent row cascades in the database; revoking first notifies session listeners. */
    @Transactional
    public void deleteAccount(User principal, HttpServletRequest request, HttpServletResponse response) {
        User user = load(principal);
        sessionService.revokeAllExcept(user.getId(), null, SessionRevokeReason.ACCOUNT_DISABLED);
        userRepository.deleteAccountById(user.getId());
        sessionIssuer.clear(request, response);
        log.info("Deleted account {} on request.", user.getId());
    }

    // --- sessions ---------------------------------------------------------------------------

    @Transactional
    public int signOutOtherSessions(User principal, HttpServletRequest request) {
        UUID current = SessionIssuer.currentSessionId(request).orElse(null);
        return sessionService.revokeAllExcept(principal.getId(), current, SessionRevokeReason.USER_REVOKED);
    }

    // --- helpers ----------------------------------------------------------------------------

    /** A rotated refresh token was replayed: the session was revoked, now tell the owner. */
    @EventListener
    public void onSessionRevoked(AuthSessionRevokedEvent event) {
        if (event.reason() != SessionRevokeReason.REUSE_DETECTED) {
            return;
        }
        userRepository.findById(event.userId()).ifPresent(user -> mailService.enqueue(MailTemplate.SECURITY_ALERT,
            address(user), new SecurityAlertModel(user.getFirstName(), SecurityAlertModel.Kind.SESSION_REUSE_DETECTED, null),
            locale(user, null)));
    }

    private void alert(User user, SecurityAlertModel.Kind kind, HttpServletRequest request) {
        mailService.enqueue(MailTemplate.SECURITY_ALERT, address(user),
            new SecurityAlertModel(user.getFirstName(), kind, DeviceLabel.of(RequestInfo.userAgent(request))),
            locale(user, request));
    }

    private User load(User principal) {
        return userRepository.findById(principal.getId())
            .orElseThrow(() -> ApiException.unauthorized("NOT_AUTHENTICATED", "Not authenticated"));
    }

    static String displayName(User user) {
        if (user.getFirstName() == null) {
            return null;
        }
        return (user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName())).strip();
    }

    private static MailAddress address(User user) {
        return new MailAddress(user.getEmail(), displayName(user));
    }

    private static Locale locale(User user, HttpServletRequest request) {
        if (user.getPreferredLocale() != null && !user.getPreferredLocale().isBlank()) {
            return Locale.forLanguageTag(user.getPreferredLocale());
        }
        return request != null && request.getLocale() != null ? request.getLocale() : Locale.GERMAN;
    }
}
