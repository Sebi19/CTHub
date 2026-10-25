package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.render.LoginLinkModel;
import org.cthub.backend.mail.render.PasswordResetModel;
import org.cthub.backend.mail.render.RegistrationConfirmationModel;
import org.cthub.backend.mail.render.WelcomeModel;
import org.cthub.backend.model.RegistrationOutcome;
import org.cthub.backend.model.User;
import org.cthub.backend.model.UserToken;
import org.cthub.backend.model.UserTokenType;
import org.cthub.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Orchestrates the account lifecycle: e-mail-only registration, verification, profile completion,
 * password reset and passwordless sign-in links.
 *
 * <p>{@link #register} deliberately reveals whether a verified account already owns the address, a
 * product decision that trades strict enumeration-resistance for a much better signup experience.
 * The credential-bearing flows ({@link #requestPasswordReset}, {@link #requestLoginLink},
 * {@link #resendVerification}) stay enumeration-safe, since leaking there buys no UX.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MailService mailService;
    private final MailProperties mailProperties;

    /** Outcome of starting a registration. */
    public record RegistrationStart(Status status, String registrationId, Short challengeNumber) {
        public enum Status {
            /** Confirmation mail sent - either for a new account or as a resend for an unverified one. */
            PENDING,
            /** A fully verified account already owns this address; the UI should offer login instead. */
            ALREADY_REGISTERED
        }

        static RegistrationStart pending(String registrationId, Short challengeNumber) {
            return new RegistrationStart(Status.PENDING, registrationId, challengeNumber);
        }

        static RegistrationStart alreadyRegistered() {
            return new RegistrationStart(Status.ALREADY_REGISTERED, null, null);
        }
    }

    /** State of a registration attempt, as observed by the polling pending page. */
    public enum RegistrationState {
        /** Mail sent, link not opened yet. */
        PENDING,
        /** The link was opened somewhere; that device is being asked to pick the number. */
        AWAITING_CONFIRMATION,
        /** Correct number picked - the waiting browser may now claim its session. */
        VERIFIED,
        /** Whoever opened the link chose to continue there instead. */
        CLAIMED_ELSEWHERE,
        /** Rejected as "that wasn't me", or too many wrong picks. */
        CANCELLED,
        /** Unknown or expired handle - the pending page should start over. */
        UNKNOWN
    }

    /** What the device that opened the e-mailed link should be shown. */
    public sealed interface ChallengeView {

        /** No browser is waiting, so there is nothing to confirm: verify straight away. */
        record None() implements ChallengeView {
        }

        /** The link was opened in the very browser that started the registration: skip the prompt. */
        record SameBrowser() implements ChallengeView {
        }

        /** Another device: ask which number the originating browser is showing. */
        record Options(List<Short> numbers, String email) implements ChallengeView {
        }

        /** Token is unusable, or the attempt is already settled. */
        record Invalid() implements ChallengeView {
        }
    }

    /**
     * Starts an e-mail-only registration: creates a disabled, nameless, passwordless account and
     * mails a verification link. The returned registration id is an opaque handle for the browser
     * that started the attempt. It is never e-mailed and can only read status, never verify.
     */
    @Transactional
    public RegistrationStart register(String email, Locale locale) {
        String normalizedEmail = normalizeEmail(email);

        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getEmailVerifiedAt() != null) {
                return RegistrationStart.alreadyRegistered();
            }
            // Unverified leftover from an earlier attempt: reuse it and send a fresh link.
            user.setPreferredLocale(locale.getLanguage());
            userRepository.save(user);
            return startAttempt(user, locale);
        }

        User user = User.builder()
            .email(normalizedEmail)
            .role(User.Role.USER)
            .enabled(false)
            .preferredLocale(locale.getLanguage())
            .build();
        userRepository.save(user);

        return startAttempt(user, locale);
    }

    /**
     * Issues the attempt handle (carrying the confirmation number shown by this browser) and mails a
     * fresh link. Order matters: the handle must exist before the mail goes out, so that whoever
     * opens the link can always resolve the challenge it belongs to.
     */
    private RegistrationStart startAttempt(User user, Locale locale) {
        TokenService.IssuedToken handle = tokenService.issueToken(
            user, UserTokenType.REGISTRATION_STATUS, TokenService.REGISTRATION_STATUS_TTL);
        handle.token().setChallengeNumber(RegistrationChallenge.newNumber());
        tokenService.save(handle.token());

        sendRegistrationConfirmation(user, locale);
        return RegistrationStart.pending(handle.rawToken(), handle.token().getChallengeNumber());
    }

    /**
     * Reports whether the account behind a registration handle has been verified yet.
     *
     * <p>Intentionally free of authentication side effects. Signing the polling browser in here
     * would let an attacker start a registration for someone else's address, keep polling, and
     * inherit a session the moment the real owner clicked the link in their inbox. Only the browser
     * that actually opens the e-mailed link gets a session - see {@link #verifyEmail}.
     */
    @Transactional(readOnly = true)
    public RegistrationState registrationState(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return RegistrationState.UNKNOWN;
        }
        return tokenService.findUsable(registrationId, UserTokenType.REGISTRATION_STATUS)
            .map(attempt -> {
                if (attempt.getChallengeOutcome() != null) {
                    return switch (attempt.getChallengeOutcome()) {
                        case CONFIRMED -> RegistrationState.VERIFIED;
                        case CLAIMED_ELSEWHERE -> RegistrationState.CLAIMED_ELSEWHERE;
                        case CANCELLED -> RegistrationState.CANCELLED;
                    };
                }
                // Legacy/no-challenge attempts (handle issued before the challenge existed) fall back
                // to the plain verification flag so they still resolve.
                if (attempt.getChallengeNumber() == null && attempt.getUser().getEmailVerifiedAt() != null) {
                    return RegistrationState.VERIFIED;
                }
                return attempt.getChallengeOpenedAt() != null
                    ? RegistrationState.AWAITING_CONFIRMATION
                    : RegistrationState.PENDING;
            })
            .orElse(RegistrationState.UNKNOWN);
    }

    /**
     * Describes what the device opening a verification link should be asked.
     *
     * <p>{@code originatingRegistrationId} is the handle from that browser's registration cookie, if
     * any. When it matches the attempt the link belongs to, the opener demonstrably <em>is</em> the
     * browser that started the registration, so there is nobody to protect and the prompt is skipped.
     */
    @Transactional
    public ChallengeView openChallenge(String rawToken, String originatingRegistrationId) {
        Optional<UserToken> tokenOpt = tokenService.findUsable(rawToken, UserTokenType.EMAIL_VERIFICATION);
        if (tokenOpt.isEmpty()) {
            return new ChallengeView.Invalid();
        }

        User user = tokenOpt.get().getUser();
        Optional<UserToken> attemptOpt = findLiveAttempt(user);
        if (attemptOpt.isEmpty()) {
            // Nobody is waiting on a pending page (a resend from the login screen, say), so there is
            // no session to hand over and nothing to confirm.
            return new ChallengeView.None();
        }

        UserToken attempt = attemptOpt.get();
        if (attempt.isChallengeSettled()) {
            return new ChallengeView.Invalid();
        }
        if (attempt.getChallengeNumber() == null) {
            return new ChallengeView.None();
        }
        if (matchesAttempt(attempt, originatingRegistrationId)) {
            return new ChallengeView.SameBrowser();
        }

        if (attempt.getChallengeOpenedAt() == null) {
            attempt.setChallengeOpenedAt(Instant.now());
            tokenService.save(attempt);
        }
        return new ChallengeView.Options(RegistrationChallenge.optionsFor(attempt), user.getEmail());
    }

    /**
     * Answers the confirmation challenge.
     *
     * <p>Deliberately grants no session to the caller: a correct pick hands control to the browser
     * that started the registration, which then calls {@link #claimRegistration}.
     *
     * <p>A wrong pick burns the verification link but leaves the attempt alive, so an honest misclick
     * costs one resend rather than a restart. Only {@link RegistrationChallenge#MAX_ATTEMPTS} wrong
     * picks cancel it outright. There is never a second pick on the same link - three options with
     * two guesses would be a worse gate than the one we are putting up.
     */
    @Transactional
    public boolean confirmChallenge(String rawToken, short number) {
        UserToken token = tokenService.findUsable(rawToken, UserTokenType.EMAIL_VERIFICATION)
            .orElseThrow(() -> new IllegalArgumentException("Verification token is not usable"));
        User user = token.getUser();
        UserToken attempt = findLiveAttempt(user)
            .orElseThrow(() -> new IllegalArgumentException("No live registration attempt"));

        if (attempt.isChallengeSettled() || attempt.getChallengeNumber() == null) {
            throw new IllegalArgumentException("Registration attempt is already settled");
        }

        if (attempt.getChallengeNumber() != number) {
            tokenService.consume(token);
            attempt.setChallengeAttempts((short) (attempt.getChallengeAttempts() + 1));
            if (attempt.getChallengeAttempts() >= RegistrationChallenge.MAX_ATTEMPTS) {
                attempt.setChallengeOutcome(RegistrationOutcome.CANCELLED);
            }
            tokenService.save(attempt);
            return false;
        }

        tokenService.consume(token);
        attempt.setChallengeOutcome(RegistrationOutcome.CONFIRMED);
        tokenService.save(attempt);
        markVerified(user);
        return true;
    }

    /**
     * Handles "that wasn't me": kills the attempt and, when the account exists only because of this
     * registration, removes it entirely so the address is free again and cannot be impersonated.
     */
    @Transactional
    public Optional<String> rejectRegistration(String rawToken) {
        Optional<UserToken> tokenOpt = tokenService.findUsable(rawToken, UserTokenType.EMAIL_VERIFICATION);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        UserToken token = tokenOpt.get();
        User user = token.getUser();
        String email = user.getEmail();
        tokenService.consume(token);

        findLiveAttempt(user).ifPresent(attempt -> {
            attempt.setChallengeOutcome(RegistrationOutcome.CANCELLED);
            tokenService.save(attempt);
        });

        // Only ever delete an account that has never been anything but a pending registration.
        if (user.getEmailVerifiedAt() == null && !user.hasPassword() && !user.isProfileComplete()) {
            userRepository.delete(user);
            log.info("Registration for a pending account was rejected by the mailbox owner; account removed.");
        }
        return Optional.of(email);
    }

    /**
     * Exchanges the originating browser's handle for its user once the challenge was confirmed there.
     * This is the one and only path by which the waiting browser gains a session, and it requires an
     * explicit {@code CONFIRMED} outcome produced by someone holding the e-mailed link.
     */
    @Transactional
    public Optional<User> claimRegistration(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return Optional.empty();
        }
        return tokenService.findUsable(registrationId, UserTokenType.REGISTRATION_STATUS)
            .filter(attempt -> attempt.getChallengeOutcome() == RegistrationOutcome.CONFIRMED)
            .map(attempt -> {
                tokenService.consume(attempt);
                return attempt.getUser();
            });
    }

    /**
     * Consumes a verification token and activates the account. Returns the activated user so the
     * caller can establish a session: possession of the e-mailed token proves inbox ownership,
     * which is exactly the bar for signing in.
     *
     * <p>This is also the "continue on this device instead" action, so any browser still waiting on
     * a pending page is told to stand down.
     */
    @Transactional
    public Optional<User> verifyEmail(String rawToken) {
        Optional<UserToken> tokenOpt = tokenService.findUsable(rawToken, UserTokenType.EMAIL_VERIFICATION);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        UserToken token = tokenOpt.get();
        User user = token.getUser();
        tokenService.consume(token);

        findLiveAttempt(user).ifPresent(attempt -> {
            if (!attempt.isChallengeSettled()) {
                attempt.setChallengeOutcome(RegistrationOutcome.CLAIMED_ELSEWHERE);
                tokenService.save(attempt);
            }
        });

        markVerified(user);
        return Optional.of(user);
    }

    private void markVerified(User user) {
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
            user.setEnabled(true);
            userRepository.save(user);
        }
    }

    /** The single usable registration handle for a user, i.e. the attempt currently in flight. */
    private Optional<UserToken> findLiveAttempt(User user) {
        return tokenService.findUsableByUser(user, UserTokenType.REGISTRATION_STATUS);
    }

    private boolean matchesAttempt(UserToken attempt, String rawRegistrationId) {
        if (rawRegistrationId == null || rawRegistrationId.isBlank()) {
            return false;
        }
        // Compared by hash rather than id: the hash is the row's natural identity and is always
        // populated, whereas an id is only assigned once the row has been flushed.
        return tokenService.findUsable(rawRegistrationId, UserTokenType.REGISTRATION_STATUS)
            .map(fromCookie -> fromCookie.getTokenHash().equals(attempt.getTokenHash()))
            .orElse(false);
    }

    /**
     * Fills in the details registration no longer asks for up front. The password is optional:
     * skipping it leaves a passwordless account that signs in via e-mailed links.
     */
    @Transactional
    public User completeProfile(User user, String firstName, String lastName, String rawPassword) {
        User managed = userRepository.findById(user.getId()).orElseThrow();
        boolean firstCompletion = !managed.isProfileComplete();

        managed.setFirstName(firstName.trim());
        managed.setLastName(lastName.trim());
        if (rawPassword != null && !rawPassword.isBlank()) {
            managed.setPassword(passwordEncoder.encode(rawPassword));
        }
        managed.setProfileCompletedAt(Instant.now());
        userRepository.save(managed);

        if (firstCompletion) {
            mailService.enqueue(
                MailTemplate.WELCOME,
                toMailAddress(managed),
                new WelcomeModel(managed.getFirstName()),
                localeOf(managed, null));
        }

        return managed;
    }

    /**
     * Resends the verification e-mail for an unverified account. Enumeration-safe: silently does
     * nothing if the address is unknown or already verified.
     */
    @Transactional
    public void resendVerification(String email, Locale locale) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            if (user.getEmailVerifiedAt() == null) {
                sendRegistrationConfirmation(user, locale);
            }
        });
    }

    /** Mails a password reset link. Enumeration-safe: unknown addresses are silently ignored. */
    @Transactional
    public void requestPasswordReset(String email, Locale locale) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            String rawToken = tokenService.issue(user, UserTokenType.PASSWORD_RESET, TokenService.PASSWORD_RESET_TTL);
            mailService.enqueue(
                MailTemplate.PASSWORD_RESET,
                toMailAddress(user),
                new PasswordResetModel(
                    user.getFirstName(),
                    mailProperties.getBaseUrl() + "/reset-password?token=" + rawToken,
                    TokenService.PASSWORD_RESET_TTL),
                localeOf(user, locale));
        });
    }

    /**
     * Consumes a reset token and sets a new password. Also verifies the account if it was still
     * pending: the token was e-mailed, so it proves inbox ownership just as well as a verification
     * link does.
     */
    @Transactional
    public Optional<User> resetPassword(String rawToken, String newPassword) {
        Optional<UserToken> tokenOpt = tokenService.findUsable(rawToken, UserTokenType.PASSWORD_RESET);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        UserToken token = tokenOpt.get();
        User user = token.getUser();
        tokenService.consume(token);

        user.setPassword(passwordEncoder.encode(newPassword));
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
            user.setEnabled(true);
        }
        userRepository.save(user);

        return Optional.of(user);
    }

    /** Mails a single-use sign-in link. Enumeration-safe: unknown addresses are silently ignored. */
    @Transactional
    public void requestLoginLink(String email, Locale locale) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            if (user.getEmailVerifiedAt() == null) {
                // Never hand out a sign-in link for an address nobody has proven they own yet;
                // finishing verification is the correct path for those accounts.
                sendRegistrationConfirmation(user, locale);
                return;
            }
            String rawToken = tokenService.issue(user, UserTokenType.LOGIN_LINK, TokenService.LOGIN_LINK_TTL);
            mailService.enqueue(
                MailTemplate.LOGIN_LINK,
                toMailAddress(user),
                new LoginLinkModel(
                    user.getFirstName(),
                    mailProperties.getBaseUrl() + "/login-link?token=" + rawToken,
                    TokenService.LOGIN_LINK_TTL),
                localeOf(user, locale));
        });
    }

    /** Consumes a sign-in link and returns the user so the caller can establish a session. */
    @Transactional
    public Optional<User> consumeLoginLink(String rawToken) {
        Optional<UserToken> tokenOpt = tokenService.findUsable(rawToken, UserTokenType.LOGIN_LINK);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        UserToken token = tokenOpt.get();
        tokenService.consume(token);
        return Optional.of(token.getUser());
    }

    private void sendRegistrationConfirmation(User user, Locale locale) {
        String rawToken = tokenService.issue(user, UserTokenType.EMAIL_VERIFICATION, TokenService.EMAIL_VERIFICATION_TTL);
        String confirmUrl = mailProperties.getBaseUrl() + "/verify-email?token=" + rawToken;

        // Deliberately no dedupe key. Outbox dedupe keys are permanent and unique, so a per-user key
        // would let an account receive exactly one confirmation mail ever and silently swallow every
        // resend. Each issued link must get its own mail; abuse is bounded by the controller's rate
        // limiter instead.
        mailService.enqueue(
            MailTemplate.REGISTRATION_CONFIRMATION,
            toMailAddress(user),
            new RegistrationConfirmationModel(user.getFirstName(), confirmUrl, TokenService.EMAIL_VERIFICATION_TTL),
            locale);
    }

    /** Prefers the account's stored preference, then the current request's locale, then German. */
    private Locale localeOf(User user, Locale requestLocale) {
        if (user.getPreferredLocale() != null) {
            return Locale.forLanguageTag(user.getPreferredLocale());
        }
        return requestLocale != null ? requestLocale : Locale.GERMAN;
    }

    private MailAddress toMailAddress(User user) {
        return new MailAddress(user.getEmail(), displayName(user));
    }

    /** Registration is e-mail-only, so a name may not exist yet; fall back to the address. */
    private String displayName(User user) {
        if (user.getFirstName() == null && user.getLastName() == null) {
            return user.getEmail();
        }
        return ((user.getFirstName() != null ? user.getFirstName() : "")
            + " "
            + (user.getLastName() != null ? user.getLastName() : "")).trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
