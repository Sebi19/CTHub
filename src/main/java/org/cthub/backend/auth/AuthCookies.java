package org.cthub.backend.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.cthub.backend.auth.session.AuthSessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

/**
 * The two auth cookies, both HttpOnly and SameSite=Lax (Lax so they survive following an e-mailed
 * link into the app):
 * <ul>
 *   <li>refresh: the session's current rotating refresh token, 90 days;</li>
 *   <li>attempt: the nonce binding a pending login challenge to the browser that started it.</li>
 * </ul>
 * In production both use the {@code __Host-} prefix, which pins them to this exact host, path
 * {@code /} and {@code Secure}.
 */
@Component
public class AuthCookies {

    private final boolean secure;
    private final String refreshName;
    private final String attemptName;

    public AuthCookies(AuthProperties properties) {
        this.secure = properties.isCookieSecure();
        this.refreshName = secure ? "__Host-ct_refresh" : "ct_refresh";
        this.attemptName = secure ? "__Host-ct_attempt" : "ct_attempt";
    }

    public String readRefresh(HttpServletRequest request) {
        return read(request, refreshName);
    }

    public void writeRefresh(HttpServletResponse response, String rawToken) {
        // As long as the longest session; the server enforces each session's own expiry.
        write(response, refreshName, rawToken, AuthSessionService.INSTALLED_APP_SESSION_LIFETIME);
    }

    public void clearRefresh(HttpServletResponse response) {
        write(response, refreshName, "", Duration.ZERO);
    }

    public String readAttempt(HttpServletRequest request) {
        return read(request, attemptName);
    }

    public void writeAttempt(HttpServletResponse response, String rawNonce, Duration ttl) {
        write(response, attemptName, rawNonce, ttl);
    }

    public void clearAttempt(HttpServletResponse response) {
        write(response, attemptName, "", Duration.ZERO);
    }

    private void write(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite("Lax")
            .maxAge(maxAge)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
            .filter(cookie -> name.equals(cookie.getName()))
            .map(Cookie::getValue)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(null);
    }
}
