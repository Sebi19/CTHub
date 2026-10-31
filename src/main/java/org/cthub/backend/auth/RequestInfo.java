package org.cthub.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Locale;

/** Small helpers for reading client details off a request. */
public final class RequestInfo {

    private RequestInfo() {
    }

    public static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }

    /**
     * Prefers the language the user explicitly selected in the UI over {@code Accept-Language},
     * which reflects browser configuration rather than an actual choice.
     */
    public static Locale locale(String requestedLocale, HttpServletRequest request) {
        if (requestedLocale != null && !requestedLocale.isBlank()) {
            Locale parsed = Locale.forLanguageTag(requestedLocale);
            if (!parsed.getLanguage().isEmpty()) {
                return parsed;
            }
        }
        Locale locale = request.getLocale();
        return locale != null ? locale : Locale.GERMAN;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
