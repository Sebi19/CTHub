package org.cthub.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.cthub.backend.model.User;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Blocks accounts that never finished the complete-profile step from using the app at all.
 *
 * <p>Signing in for the first time creates the account from nothing but the e-mail address, so a user who abandons the flow afterwards is signed in with no name. A real name is
 * mandatory, so every {@code /api/**} call outside the escape hatches below is rejected with
 * {@code PROFILE_INCOMPLETE} - reads included - which the frontend turns into a redirect to the
 * completion page.
 */
@Component
public class ProfileCompletionInterceptor implements HandlerInterceptor {

    /**
     * A half-finished account must still be able to call every auth endpoint to get itself unstuck
     * (most importantly {@code /api/auth/me}, without which the frontend cannot even discover that
     * the profile is incomplete, and {@code /api/auth/complete-profile} itself).
     */
    private static final String ALLOWED_PREFIX = "/api/auth/";

    /** Mirrors {@code ErrorResponseDto}; hand-written because the payload is entirely constant. */
    private static final String PROFILE_INCOMPLETE_BODY =
        "{\"code\":\"PROFILE_INCOMPLETE\",\"message\":\"Please complete your profile before continuing.\"}";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws IOException {

        // Preflights carry no credentials and must never be answered with an error, or the browser
        // reports a CORS failure instead of the 403 the actual request would have produced.
        if (HttpMethod.OPTIONS.matches(request.getMethod())
            || request.getRequestURI().startsWith(ALLOWED_PREFIX)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user)
            || user.isProfileComplete()) {
            return true;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(PROFILE_INCOMPLETE_BODY);
        return false;
    }
}
