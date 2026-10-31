package org.cthub.backend.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.auth.AuthCookies;
import org.cthub.backend.auth.RequestInfo;
import org.cthub.backend.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Signs a browser in and out. The HTTP session is only a short-lived access cache that carries the
 * security context and the id of the {@link AuthSession} behind it; the refresh cookie restores it.
 */
@Component
@RequiredArgsConstructor
public class SessionIssuer {

    /** HTTP-session and request attribute holding the current {@link AuthSession} id. */
    public static final String SESSION_ATTRIBUTE = "AUTH_SESSION_ID";

    private final AuthSessionService sessionService;
    private final AuthCookies cookies;

    /** Starts a new 90-day session for {@code user} in this browser, replacing any previous one. */
    public AuthSession signIn(User user, Set<AuthMethod> methods, HttpServletRequest request,
                              HttpServletResponse response) {
        // Whoever was signed in here before (possibly another account) is signed out for good.
        currentSessionId(request).ifPresent(id -> sessionService.revoke(id, SessionRevokeReason.REPLACED));
        sessionService.revokeByRefreshToken(cookies.readRefresh(request), SessionRevokeReason.REPLACED);

        AuthSessionService.Issued issued = sessionService.create(
            user, methods, RequestInfo.userAgent(request), RequestInfo.clientIp(request));
        establish(user, issued.session().getId(), request, true);
        cookies.writeRefresh(response, issued.refreshToken());
        return issued.session();
    }

    public void signOut(HttpServletRequest request, HttpServletResponse response, SessionRevokeReason reason) {
        Optional<UUID> current = currentSessionId(request);
        current.ifPresent(id -> sessionService.revoke(id, reason));
        if (current.isEmpty()) {
            sessionService.revokeByRefreshToken(cookies.readRefresh(request), reason);
        }
        clear(request, response);
    }

    /** Drops this browser's credentials without touching the server-side session. */
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
        SecurityContextHolder.clearContext();
        request.removeAttribute(SESSION_ATTRIBUTE);
        cookies.clearRefresh(response);
    }

    /**
     * Binds {@code user} and the session id to this request; with {@code persistent}, also to a new
     * HTTP session (a fresh id, which defeats session fixation).
     */
    void establish(User user, UUID sessionId, HttpServletRequest request, boolean persistent) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        request.setAttribute(SESSION_ATTRIBUTE, sessionId);

        if (persistent) {
            HttpSession old = request.getSession(false);
            if (old != null) {
                old.invalidate();
            }
            HttpSession httpSession = request.getSession(true);
            httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            httpSession.setAttribute(SESSION_ATTRIBUTE, sessionId);
        }
    }

    /** Swaps the cached principal for a freshly loaded one, e.g. after a profile change. */
    public void refreshPrincipal(User user, HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null && httpSession.getAttribute(SESSION_ATTRIBUTE) != null) {
            httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        }
    }

    public static Optional<UUID> currentSessionId(HttpServletRequest request) {
        return request.getAttribute(SESSION_ATTRIBUTE) instanceof UUID id ? Optional.of(id) : Optional.empty();
    }
}
