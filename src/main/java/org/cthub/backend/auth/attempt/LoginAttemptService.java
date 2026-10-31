package org.cthub.backend.auth.attempt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.auth.DeviceLabel;
import org.cthub.backend.auth.SecureTokens;
import org.cthub.backend.auth.session.AuthMethod;
import org.cthub.backend.auth.session.AuthSessionService;
import org.cthub.backend.auth.session.SessionIssuer;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.render.LoginLinkModel;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.push.PushDispatcher;
import org.cthub.backend.notification.push.PushMessage;
import org.cthub.backend.notification.push.PushMessageComposer;
import org.cthub.backend.notification.push.PushTarget;
import org.cthub.backend.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The number-matching verification state machine shared by login, account creation and step-up.
 *
 * <p>Invariants:
 * <ul>
 *   <li>Nothing here changes state on a plain read; every GET-backed method is side-effect free, so
 *       mail scanners prefetching a link can neither consume nor approve it.</li>
 *   <li>Browser A (holder of the attempt nonce) only ever gets a session through
 *       {@link #complete}, which requires an {@link LoginAttemptStatus#APPROVED} attempt. Polling
 *       never signs anybody in.</li>
 *   <li>Picking a wrong number rejects the whole attempt on every device; there is no second guess.</li>
 *   <li>An address without an account behaves exactly like a passwordless account; the account is
 *       only created once the mailbox owner approved.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    public static final Duration ATTEMPT_TTL = Duration.ofMinutes(15);
    /** How long a push gets before the e-mailed link is sent as well (pushes may hit dead devices). */
    public static final Duration PUSH_EMAIL_DELAY = Duration.ofSeconds(15);
    /** Within this window a resend request is silently ignored. */
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    /** Browser A's time to pick up an approval that landed just before the attempt expired. */
    static final Duration COMPLETION_GRACE = Duration.ofMinutes(2);
    private static final int MAX_USER_AGENT = 512;
    private static final int MAX_IP = 64;
    private static final int MAX_ENDPOINT = 2048;

    private final LoginAttemptRepository repository;
    private final UserRepository userRepository;
    private final AuthSessionService sessionService;
    private final SessionIssuer sessionIssuer;
    private final PushDispatcher pushDispatcher;
    private final PushMessageComposer pushComposer;
    private final MailService mailService;
    private final MailProperties mailProperties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** A freshly started attempt and the raw nonce for browser A's cookie. */
    public record Started(LoginAttempt attempt, String nonce) {
    }

    /** Outcome of a number pick. */
    public enum PickResult {
        APPROVED,
        REJECTED
    }

    /** What the page behind an e-mailed link should show. */
    public record LinkView(Kind kind, LoginAttempt attempt, List<Integer> options) {
        public enum Kind {
            /** Opened in browser A itself: completing needs no number. */
            SAME_BROWSER,
            /** Opened elsewhere: approve browser A by number, or sign in here instead. */
            CROSS,
            INVALID
        }
    }

    record PushRequested(List<PushTarget> targets, PushMessage message, Duration ttl) {
    }

    record Settled(long userId, String tag) {
    }

    // --- starting ---------------------------------------------------------------------------

    @Transactional
    public Started startLogin(String normalizedEmail, Locale locale, String userAgent, String ip,
                              String pushEndpoint, String previousNonce) {
        Optional<User> user = userRepository.findByEmail(normalizedEmail);
        return start(LoginAttemptPurpose.LOGIN, normalizedEmail, user.orElse(null), null, locale, userAgent, ip,
            pushEndpoint, previousNonce);
    }

    @Transactional
    public Started startStepUp(User user, UUID sessionId, Locale locale, String userAgent, String ip,
                               String pushEndpoint, String previousNonce) {
        User fresh = userRepository.findById(user.getId())
            .orElseThrow(() -> ApiException.unauthorized("NOT_AUTHENTICATED", "Not authenticated"));
        return start(LoginAttemptPurpose.STEP_UP, fresh.getEmail(), fresh, sessionId, locale, userAgent, ip,
            pushEndpoint, previousNonce);
    }

    private Started start(LoginAttemptPurpose purpose, String email, User user, UUID stepUpSessionId, Locale locale,
                          String userAgent, String ip, String pushEndpoint, String previousNonce) {
        Instant now = clock.instant();
        cancelPrevious(previousNonce, now);

        String excluded = pushEndpoint == null || pushEndpoint.isBlank() || pushEndpoint.length() > MAX_ENDPOINT
            ? null : pushEndpoint.strip();
        // Disabled accounts get no push; the mailbox owner still receives the (then useless) link, so
        // the response looks exactly like any other address.
        List<PushTarget> targets = user != null && user.isEnabled()
            ? pushDispatcher.loginApprovalTargets(user.getId(), excluded)
            : List.of();

        String nonce = SecureTokens.generate();
        LoginAttempt attempt = repository.save(LoginAttempt.builder()
            .id(UUID.randomUUID())
            .purpose(purpose)
            .email(email)
            .user(user)
            .stepUpSessionId(stepUpSessionId)
            .nonceHash(SecureTokens.hash(nonce))
            .challengeNumber(LoginChallenge.newNumber())
            .status(LoginAttemptStatus.PENDING)
            .locale(locale.toLanguageTag())
            .initiatorUserAgent(truncate(userAgent, MAX_USER_AGENT))
            .initiatorIp(truncate(ip, MAX_IP))
            .excludedPushEndpoint(excluded)
            .createdAt(now)
            .expiresAt(now.plus(ATTEMPT_TTL))
            .pushedAt(targets.isEmpty() ? null : now)
            .emailDueAt(targets.isEmpty() ? now : now.plus(PUSH_EMAIL_DELAY))
            .build());

        if (targets.isEmpty()) {
            sendLink(attempt, now);
        } else {
            PushMessage message = pushComposer.loginApproval(locale, attempt.getId(),
                DeviceLabel.of(userAgent), attempt.pushTag());
            events.publishEvent(new PushRequested(targets, message, ATTEMPT_TTL));
        }
        return new Started(attempt, nonce);
    }

    private void cancelPrevious(String previousNonce, Instant now) {
        if (previousNonce == null || previousNonce.isBlank()) {
            return;
        }
        repository.findByNonceHash(SecureTokens.hash(previousNonce))
            .flatMap(previous -> repository.findByIdForUpdate(previous.getId()))
            .filter(previous -> previous.isPending(now))
            .ifPresent(previous -> settle(previous, LoginAttemptStatus.CANCELLED, now));
    }

    // --- e-mail delivery --------------------------------------------------------------------

    /** Called by {@link LoginAttemptEmailJob} once the push head start ran out. */
    @Transactional
    public void sendLinkIfDue(UUID attemptId) {
        Instant now = clock.instant();
        repository.findByIdForUpdate(attemptId)
            .filter(attempt -> attempt.isPending(now) && attempt.getEmailSentAt() == null
                && attempt.getEmailDueAt() != null && !attempt.getEmailDueAt().isAfter(now))
            .ifPresent(attempt -> sendLink(attempt, now));
    }

    /**
     * "Send the link now" from browser A. Within {@link #RESEND_COOLDOWN} of the last mail this is a
     * silent no-op, so the response never reveals whether a mail had already gone out (it goes out
     * immediately for unknown addresses but only after the push head start for push users).
     */
    @Transactional
    public void requestEmail(String nonce) {
        Instant now = clock.instant();
        LoginAttempt attempt = lockByNonce(nonce);
        requirePending(attempt, now);
        if (attempt.getEmailSentAt() != null && attempt.getEmailSentAt().plus(RESEND_COOLDOWN).isAfter(now)) {
            return;
        }
        sendLink(attempt, now);
    }

    /** Issues a fresh link token (invalidating earlier links) and queues the mail. */
    private void sendLink(LoginAttempt attempt, Instant now) {
        String token = SecureTokens.generate();
        attempt.setLinkTokenHash(SecureTokens.hash(token));
        attempt.setEmailSentAt(now);
        attempt.setEmailDueAt(null);

        User user = attempt.getUser();
        String firstName = user != null ? user.getFirstName() : null;
        String name = user != null && user.getFirstName() != null
            ? (user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName())).strip()
            : null;
        long seconds = Math.max(60, Duration.between(now, attempt.getExpiresAt()).toSeconds());
        Duration validity = Duration.ofMinutes((seconds + 59) / 60);
        String url = mailProperties.getBaseUrl() + "/auth/link?token=" + token;
        mailService.enqueue(MailTemplate.LOGIN_LINK, new MailAddress(attempt.getEmail(), name),
            new LoginLinkModel(firstName, url, validity), Locale.forLanguageTag(attempt.getLocale()));
    }

    // --- browser A --------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<LoginAttempt> findByNonce(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            return Optional.empty();
        }
        return repository.findByNonceHash(SecureTokens.hash(nonce));
    }

    @Transactional
    public void cancel(String nonce) {
        Instant now = clock.instant();
        findByNonce(nonce)
            .flatMap(attempt -> repository.findByIdForUpdate(attempt.getId()))
            .filter(attempt -> attempt.isPending(now) || attempt.getStatus() == LoginAttemptStatus.APPROVED)
            .ifPresent(attempt -> settle(attempt, LoginAttemptStatus.CANCELLED, now));
    }

    /**
     * Browser A picks up an approval: signs it in ({@code LOGIN}) or marks its session as freshly
     * verified ({@code STEP_UP}). The only way browser A ever gets a session out of an attempt.
     */
    @Transactional
    public LoginAttemptPurpose complete(String nonce, HttpServletRequest request, HttpServletResponse response) {
        Instant now = clock.instant();
        LoginAttempt attempt = lockByNonce(nonce);
        if (attempt.getStatus() != LoginAttemptStatus.APPROVED || attempt.isExpired(now)) {
            throw conflict("ATTEMPT_NOT_APPROVED", "This sign-in has not been approved.");
        }
        AuthMethod method = attempt.getResolvedVia() == LoginAttemptResolution.PUSH ? AuthMethod.PUSH : AuthMethod.EMAIL;
        finishInInitiator(attempt, method, now, request, response);
        return attempt.getPurpose();
    }

    private void finishInInitiator(LoginAttempt attempt, AuthMethod method, Instant now,
                                   HttpServletRequest request, HttpServletResponse response) {
        if (attempt.getPurpose() == LoginAttemptPurpose.STEP_UP) {
            UUID current = SessionIssuer.currentSessionId(request).orElse(null);
            if (current == null || !current.equals(attempt.getStepUpSessionId())) {
                throw ApiException.forbidden("STEP_UP_SESSION_MISMATCH", "This verification belongs to another session.");
            }
            sessionService.markSteppedUp(current);
        } else {
            sessionIssuer.signIn(attempt.getUser(), EnumSet.of(method), request, response);
        }
        attempt.setStatus(LoginAttemptStatus.COMPLETED);
        if (attempt.getResolvedAt() == null) {
            attempt.setResolvedAt(now);
        }
    }

    // --- e-mailed link ----------------------------------------------------------------------

    /** Side-effect free: safe for scanners and prefetchers. */
    @Transactional(readOnly = true)
    public LinkView viewLink(String token, String nonce) {
        Instant now = clock.instant();
        Optional<LoginAttempt> found = findByLinkToken(token).filter(attempt -> attempt.isPending(now));
        if (found.isEmpty()) {
            return new LinkView(LinkView.Kind.INVALID, null, List.of());
        }
        LoginAttempt attempt = found.get();
        if (matchesNonce(attempt, nonce)) {
            return new LinkView(LinkView.Kind.SAME_BROWSER, attempt, List.of());
        }
        return new LinkView(LinkView.Kind.CROSS, attempt, options(attempt));
    }

    /** Lets browser A tell its user to look at the other device. Called by the page, never on GET. */
    @Transactional
    public void markLinkOpened(String token) {
        Instant now = clock.instant();
        lockByLinkToken(token)
            .filter(attempt -> attempt.isPending(now) && attempt.getLinkOpenedAt() == null)
            .ifPresent(attempt -> attempt.setLinkOpenedAt(now));
    }

    /** The link was opened in browser A itself: the nonce cookie proves it, no number needed. */
    @Transactional
    public LoginAttemptPurpose completeSameBrowser(String token, String nonce, HttpServletRequest request,
                                                   HttpServletResponse response) {
        Instant now = clock.instant();
        LoginAttempt attempt = lockByLinkToken(token).orElseThrow(LoginAttemptService::invalidLink);
        requirePending(attempt, now);
        if (!matchesNonce(attempt, nonce)) {
            throw ApiException.forbidden("NOT_INITIATING_BROWSER", "This link was opened in another browser.");
        }
        if (!approve(attempt, LoginAttemptResolution.SAME_BROWSER, SessionIssuer.currentSessionId(request).orElse(null), now)) {
            throw accountUnavailable();
        }
        finishInInitiator(attempt, AuthMethod.EMAIL, now, request, response);
        return attempt.getPurpose();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public PickResult approveViaLink(String token, int number, UUID approverSessionId) {
        Instant now = clock.instant();
        LoginAttempt attempt = lockByLinkToken(token).orElseThrow(LoginAttemptService::invalidLink);
        requirePending(attempt, now);
        return pick(attempt, number, LoginAttemptResolution.EMAIL, approverSessionId, now);
    }

    /**
     * "Sign in on this browser instead": cancels browser A and signs in the browser holding the link.
     * Not available for step-up, which is about one specific session.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void claimHere(String token, HttpServletRequest request, HttpServletResponse response) {
        Instant now = clock.instant();
        LoginAttempt attempt = lockByLinkToken(token).orElseThrow(LoginAttemptService::invalidLink);
        requirePending(attempt, now);
        if (attempt.getPurpose() != LoginAttemptPurpose.LOGIN) {
            throw ApiException.badRequest("CLAIM_NOT_ALLOWED", "This verification cannot be moved to another browser.");
        }
        if (!approve(attempt, LoginAttemptResolution.CLAIMED_HERE, null, now)) {
            throw accountUnavailable();
        }
        attempt.setStatus(LoginAttemptStatus.CLAIMED_ELSEWHERE);
        sessionIssuer.signIn(attempt.getUser(), EnumSet.of(AuthMethod.EMAIL), request, response);
        attempt.setResolvedBySessionId(SessionIssuer.currentSessionId(request).orElse(null));
    }

    @Transactional
    public void rejectViaLink(String token) {
        Instant now = clock.instant();
        lockByLinkToken(token)
            .filter(attempt -> attempt.isPending(now))
            .ifPresent(attempt -> settle(attempt, LoginAttemptStatus.REJECTED, now));
    }

    // --- push approval on a signed-in device ------------------------------------------------

    /** Side-effect free. Only the attempt's own account may see it, never from the session being verified. */
    @Transactional(readOnly = true)
    public LinkView viewApproval(UUID attemptId, long userId, UUID approverSessionId) {
        LoginAttempt attempt = requireApprovable(repository.findById(attemptId), userId, approverSessionId);
        return new LinkView(LinkView.Kind.CROSS, attempt, options(attempt));
    }

    /** The notification was tapped: the owner is on it, so the fallback mail is no longer needed. */
    @Transactional
    public void markPushOpened(UUID attemptId, long userId, UUID approverSessionId) {
        Instant now = clock.instant();
        LoginAttempt attempt = requireApprovable(repository.findByIdForUpdate(attemptId), userId, approverSessionId);
        if (attempt.getPushOpenedAt() == null) {
            attempt.setPushOpenedAt(now);
        }
        if (attempt.getEmailSentAt() == null) {
            attempt.setEmailDueAt(null);
        }
    }

    @Transactional(noRollbackFor = ApiException.class)
    public PickResult approveViaPush(UUID attemptId, int number, long userId, UUID approverSessionId) {
        Instant now = clock.instant();
        LoginAttempt attempt = requireApprovable(repository.findByIdForUpdate(attemptId), userId, approverSessionId);
        return pick(attempt, number, LoginAttemptResolution.PUSH, approverSessionId, now);
    }

    @Transactional
    public void rejectViaPush(UUID attemptId, long userId, UUID approverSessionId) {
        Instant now = clock.instant();
        LoginAttempt attempt = requireApprovable(repository.findByIdForUpdate(attemptId), userId, approverSessionId);
        settle(attempt, LoginAttemptStatus.REJECTED, now);
    }

    private LoginAttempt requireApprovable(Optional<LoginAttempt> found, long userId, UUID approverSessionId) {
        Instant now = clock.instant();
        // One error for every reason, so the approval endpoint is no oracle for other accounts' attempts.
        return found
            .filter(attempt -> attempt.isPending(now))
            .filter(attempt -> attempt.getUser() != null && attempt.getUser().getId() == userId)
            .filter(attempt -> approverSessionId != null && !approverSessionId.equals(attempt.getStepUpSessionId()))
            .orElseThrow(() -> ApiException.notFound("ATTEMPT_NOT_FOUND", "This sign-in request is no longer valid."));
    }

    // --- transitions ------------------------------------------------------------------------

    private PickResult pick(LoginAttempt attempt, int number, LoginAttemptResolution via, UUID approverSessionId,
                            Instant now) {
        if (number != attempt.getChallengeNumber()) {
            settle(attempt, LoginAttemptStatus.REJECTED, now);
            return PickResult.REJECTED;
        }
        if (!approve(attempt, via, approverSessionId, now)) {
            throw accountUnavailable();
        }
        return PickResult.APPROVED;
    }

    /**
     * Marks the mailbox / device owner's approval and makes sure an account exists for it: an
     * address without one gets it now, a never-verified legacy account is verified. A disabled
     * account cancels the attempt instead ({@code false}).
     */
    private boolean approve(LoginAttempt attempt, LoginAttemptResolution via, UUID approverSessionId, Instant now) {
        User user = attempt.getUser() != null ? attempt.getUser()
            : userRepository.findByEmail(attempt.getEmail()).orElse(null);
        if (user == null) {
            user = userRepository.save(User.builder()
                .email(attempt.getEmail())
                .role(User.Role.USER)
                .enabled(true)
                .emailVerifiedAt(now)
                .preferredLocale(attempt.getLocale())
                .build());
            log.info("Created account {} after its first approved sign-in.", user.getId());
        } else if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(now);
            user.setEnabled(true);
        } else if (!user.isEnabled()) {
            attempt.setUser(user);
            settle(attempt, LoginAttemptStatus.CANCELLED, now);
            return false;
        }
        attempt.setUser(user);
        attempt.setResolvedVia(via);
        attempt.setResolvedBySessionId(approverSessionId);
        attempt.setResolvedAt(now);
        attempt.setStatus(LoginAttemptStatus.APPROVED);
        attempt.setEmailDueAt(null);
        Instant pickupDeadline = now.plus(COMPLETION_GRACE);
        if (attempt.getExpiresAt().isBefore(pickupDeadline)) {
            attempt.setExpiresAt(pickupDeadline);
        }
        dismissPushes(attempt);
        return true;
    }

    private void settle(LoginAttempt attempt, LoginAttemptStatus status, Instant now) {
        attempt.setStatus(status);
        attempt.setResolvedAt(now);
        attempt.setEmailDueAt(null);
        dismissPushes(attempt);
    }

    private void dismissPushes(LoginAttempt attempt) {
        if (attempt.getPushedAt() != null && attempt.getUser() != null) {
            events.publishEvent(new Settled(attempt.getUser().getId(), attempt.pushTag()));
        }
    }

    @TransactionalEventListener
    public void onPushRequested(PushRequested event) {
        pushDispatcher.sendLoginApproval(event.targets(), event.message(), event.ttl());
    }

    @TransactionalEventListener
    public void onSettled(Settled event) {
        pushDispatcher.dismiss(event.userId(), event.tag());
    }

    // --- helpers ----------------------------------------------------------------------------

    public static List<Integer> options(LoginAttempt attempt) {
        return LoginChallenge.optionsFor(attempt.getChallengeNumber(), attempt.getId().toString());
    }

    private LoginAttempt lockByNonce(String nonce) {
        return findByNonce(nonce)
            .flatMap(attempt -> repository.findByIdForUpdate(attempt.getId()))
            .orElseThrow(() -> ApiException.notFound("ATTEMPT_NOT_FOUND", "No sign-in is in progress in this browser."));
    }

    private Optional<LoginAttempt> findByLinkToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return repository.findByLinkTokenHash(SecureTokens.hash(token));
    }

    private Optional<LoginAttempt> lockByLinkToken(String token) {
        return findByLinkToken(token).flatMap(attempt -> repository.findByIdForUpdate(attempt.getId()));
    }

    private static boolean matchesNonce(LoginAttempt attempt, String nonce) {
        return nonce != null && !nonce.isBlank()
            && java.security.MessageDigest.isEqual(
                SecureTokens.hash(nonce).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                attempt.getNonceHash().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static void requirePending(LoginAttempt attempt, Instant now) {
        if (!attempt.isPending(now)) {
            throw conflict("ATTEMPT_NOT_PENDING", "This sign-in request is no longer valid.");
        }
    }

    private static ApiException invalidLink() {
        return ApiException.notFound("LINK_INVALID", "This link is invalid or has expired.");
    }

    private static ApiException accountUnavailable() {
        return ApiException.forbidden("ACCOUNT_DISABLED", "This account is disabled.");
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
