package org.cthub.backend.auth.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.auth.AuthCookies;
import org.cthub.backend.auth.RequestInfo;
import org.cthub.backend.model.User;
import org.cthub.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs right after Spring Security has loaded the context from the HTTP session.
 * <ul>
 *   <li>Signed-in HTTP session: checks that the {@link AuthSession} behind it is still live (it may
 *       have been revoked from another device) and exposes its id as a request attribute.</li>
 *   <li>No HTTP session but a refresh cookie: refreshes (see {@link AuthSessionService#refresh}).
 *       A rotation re-creates the HTTP session and sets the new cookie; the grace case authenticates
 *       only this request.</li>
 * </ul>
 * Deliberately not a Spring bean, so the servlet container doesn't register it a second time.
 */
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSessionService sessionService;
    private final SessionIssuer issuer;
    private final AuthCookies cookies;
    private final UserRepository userRepository;

    public SessionAuthenticationFilter(AuthSessionService sessionService, SessionIssuer issuer, AuthCookies cookies,
                                       UserRepository userRepository) {
        this.sessionService = sessionService;
        this.issuer = issuer;
        this.cookies = cookies;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            Object id = httpSession.getAttribute(SessionIssuer.SESSION_ATTRIBUTE);
            Object context = httpSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (id instanceof UUID sessionId && context instanceof SecurityContext) {
                if (sessionService.isLive(sessionId)) {
                    request.setAttribute(SessionIssuer.SESSION_ATTRIBUTE, sessionId);
                    chain.doFilter(request, response);
                    return;
                }
                issuer.clear(request, response);
                chain.doFilter(request, response);
                return;
            } else if (context != null) {
                // An authenticated HTTP session without a server-side session cannot be revoked; drop it.
                httpSession.invalidate();
                SecurityContextHolder.clearContext();
            }
        }

        String refreshToken = cookies.readRefresh(request);
        if (refreshToken != null) {
            restore(refreshToken, request, response);
        }
        chain.doFilter(request, response);
    }

    private void restore(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        AuthSessionService.RefreshOutcome outcome;
        try {
            outcome = sessionService.refresh(refreshToken, RequestInfo.userAgent(request), RequestInfo.clientIp(request));
        } catch (RuntimeException e) {
            // Never fail the request because of the refresh; it just proceeds unauthenticated.
            log.warn("Session refresh failed: {}", e.toString());
            return;
        }
        switch (outcome) {
            case AuthSessionService.RefreshOutcome.Rotated rotated -> activeUser(rotated.userId(), rotated.sessionId())
                .ifPresentOrElse(user -> {
                    issuer.establish(user, rotated.sessionId(), request, true);
                    cookies.writeRefresh(response, rotated.refreshToken());
                }, () -> cookies.clearRefresh(response));
            case AuthSessionService.RefreshOutcome.Grace grace -> activeUser(grace.userId(), grace.sessionId())
                .ifPresent(user -> issuer.establish(user, grace.sessionId(), request, false));
            case AuthSessionService.RefreshOutcome.Invalid ignored -> cookies.clearRefresh(response);
        }
    }

    private Optional<User> activeUser(long userId, UUID sessionId) {
        Optional<User> user = userRepository.findById(userId).filter(User::isEnabled);
        if (user.isEmpty()) {
            sessionService.revoke(sessionId, SessionRevokeReason.ACCOUNT_DISABLED);
        }
        return user;
    }
}
