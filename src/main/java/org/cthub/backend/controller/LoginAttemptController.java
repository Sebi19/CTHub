package org.cthub.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.auth.AuthCookies;
import org.cthub.backend.auth.RequestInfo;
import org.cthub.backend.auth.attempt.LoginAttemptPurpose;
import org.cthub.backend.auth.attempt.LoginAttemptService;
import org.cthub.backend.auth.session.SessionIssuer;
import org.cthub.backend.dto.auth.ApprovalPickRequestDto;
import org.cthub.backend.dto.auth.AttemptCompletionDto;
import org.cthub.backend.dto.auth.ChallengePickResult;
import org.cthub.backend.dto.auth.ChallengePickResultDto;
import org.cthub.backend.dto.auth.ChallengeViewDto;
import org.cthub.backend.dto.auth.LinkApproveRequestDto;
import org.cthub.backend.dto.auth.LinkTokenRequestDto;
import org.cthub.backend.dto.auth.LoginAttemptStateDto;
import org.cthub.backend.dto.auth.UserDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.mapper.AuthMapper;
import org.cthub.backend.mapper.UserMapper;
import org.cthub.backend.model.User;
import org.cthub.backend.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * The number-matching challenge from all three sides:
 * <ul>
 *   <li>browser A (holder of the attempt cookie) polls and picks up the approval;</li>
 *   <li>the e-mailed link - every GET is side-effect free, so mail scanners can't spend it;</li>
 *   <li>a signed-in device answering the push notification.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class LoginAttemptController {

    private final LoginAttemptService attemptService;
    private final AuthCookies cookies;
    private final RateLimiterService rateLimiter;
    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    private final Clock clock;

    // --- browser A --------------------------------------------------------------------------

    /** Poll target. Never signs anybody in; see {@link #completeCurrent}. */
    @GetMapping("/attempts/current")
    public LoginAttemptStateDto getCurrentAttempt(HttpServletRequest request) {
        return attemptService.findByNonce(cookies.readAttempt(request))
            .map(attempt -> authMapper.toStateDto(attempt, clock.instant()))
            .orElseThrow(() -> ApiException.notFound("ATTEMPT_NOT_FOUND", "No sign-in is in progress in this browser."));
    }

    @PostMapping("/attempts/current/complete")
    public AttemptCompletionDto completeCurrentAttempt(HttpServletRequest request, HttpServletResponse response) {
        LoginAttemptPurpose purpose = attemptService.complete(cookies.readAttempt(request), request, response);
        cookies.clearAttempt(response);
        return new AttemptCompletionDto(purpose, currentUserDto());
    }

    /** "Send me the link by e-mail (again)". Silently ignored within the resend cooldown. */
    @PostMapping("/attempts/current/email")
    public ResponseEntity<Void> sendAttemptEmail(HttpServletRequest request) {
        limit("auth:resend:ip:" + RequestInfo.clientIp(request), 10, Duration.ofHours(1));
        attemptService.requestEmail(cookies.readAttempt(request));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/attempts/current")
    public ResponseEntity<Void> cancelCurrentAttempt(HttpServletRequest request, HttpServletResponse response) {
        attemptService.cancel(cookies.readAttempt(request));
        cookies.clearAttempt(response);
        return ResponseEntity.noContent().build();
    }

    // --- e-mailed link ----------------------------------------------------------------------

    /** Read-only. Tells the page whether this is browser A itself or another browser/device. */
    @GetMapping("/link")
    public ChallengeViewDto viewLink(@RequestParam String token, HttpServletRequest request) {
        return authMapper.toChallengeViewDto(attemptService.viewLink(token, cookies.readAttempt(request)));
    }

    /** Sent by the landing page's script (scanners don't run it) so browser A can say "check the other device". */
    @PostMapping("/link/opened")
    public ResponseEntity<Void> linkOpened(@Valid @RequestBody LinkTokenRequestDto body, HttpServletRequest request) {
        limitLink(request);
        attemptService.markLinkOpened(body.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/link/complete")
    public AttemptCompletionDto completeLink(@Valid @RequestBody LinkTokenRequestDto body, HttpServletRequest request,
                                             HttpServletResponse response) {
        limitLink(request);
        LoginAttemptPurpose purpose = attemptService.completeSameBrowser(body.token(), cookies.readAttempt(request),
            request, response);
        cookies.clearAttempt(response);
        return new AttemptCompletionDto(purpose, currentUserDto());
    }

    @PostMapping("/link/approve")
    public ChallengePickResultDto approveLink(@Valid @RequestBody LinkApproveRequestDto body, HttpServletRequest request) {
        limitLink(request);
        LoginAttemptService.PickResult result = attemptService.approveViaLink(body.token(), body.number(),
            SessionIssuer.currentSessionId(request).orElse(null));
        return toDto(result);
    }

    /** "Sign in on this browser instead": cancels browser A's request. */
    @PostMapping("/link/claim-here")
    public UserDto claimLinkHere(@Valid @RequestBody LinkTokenRequestDto body, HttpServletRequest request,
                             HttpServletResponse response) {
        limitLink(request);
        attemptService.claimHere(body.token(), request, response);
        return currentUserDto();
    }

    @PostMapping("/link/reject")
    public ResponseEntity<Void> rejectLink(@Valid @RequestBody LinkTokenRequestDto body, HttpServletRequest request) {
        limitLink(request);
        attemptService.rejectViaLink(body.token());
        return ResponseEntity.noContent().build();
    }

    // --- push approval on a signed-in device ------------------------------------------------

    @GetMapping("/approvals/{attemptId}")
    @PreAuthorize("isAuthenticated()")
    public ChallengeViewDto viewPushApproval(@PathVariable UUID attemptId, @AuthenticationPrincipal User user,
                                         HttpServletRequest request) {
        return authMapper.toChallengeViewDto(
            attemptService.viewApproval(attemptId, user.getId(), approverSession(request)));
    }

    @PostMapping("/approvals/{attemptId}/opened")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> pushApprovalOpened(@PathVariable UUID attemptId, @AuthenticationPrincipal User user,
                                               HttpServletRequest request) {
        attemptService.markPushOpened(attemptId, user.getId(), approverSession(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/approvals/{attemptId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ChallengePickResultDto approvePush(@PathVariable UUID attemptId, @Valid @RequestBody ApprovalPickRequestDto body,
                                          @AuthenticationPrincipal User user, HttpServletRequest request) {
        return toDto(attemptService.approveViaPush(attemptId, body.number(), user.getId(), approverSession(request)));
    }

    @PostMapping("/approvals/{attemptId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> rejectPush(@PathVariable UUID attemptId, @AuthenticationPrincipal User user,
                                       HttpServletRequest request) {
        attemptService.rejectViaPush(attemptId, user.getId(), approverSession(request));
        return ResponseEntity.noContent().build();
    }

    // --- helpers ----------------------------------------------------------------------------

    private static UUID approverSession(HttpServletRequest request) {
        return SessionIssuer.currentSessionId(request).orElse(null);
    }

    private static ChallengePickResultDto toDto(LoginAttemptService.PickResult result) {
        return new ChallengePickResultDto(result == LoginAttemptService.PickResult.APPROVED
            ? ChallengePickResult.APPROVED : ChallengePickResult.REJECTED);
    }

    private UserDto currentUserDto() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null
            : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return userMapper.toUserDto(user);
        }
        throw ApiException.unauthorized("NOT_AUTHENTICATED", "Not authenticated");
    }

    private void limitLink(HttpServletRequest request) {
        limit("auth:link:ip:" + RequestInfo.clientIp(request), 30, Duration.ofMinutes(10));
    }

    private void limit(String key, int max, Duration window) {
        if (!rateLimiter.tryAcquire(key, max, window)) {
            throw ApiException.tooManyRequests("Too many requests. Please try again later.");
        }
    }
}
