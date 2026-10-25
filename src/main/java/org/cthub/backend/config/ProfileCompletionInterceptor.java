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
import java.util.Set;

/**
 * Blocks accounts that never finished the complete-profile step from using the app at all.
 *
 * <p>Registration only collects an e-mail address, so a user who abandons the flow after clicking
 * the verification link ends up signed in with no name and possibly no password. A real name is
 * mandatory, so every {@code /api/**} call outside the escape hatches below is rejected with
 * {@code PROFILE_INCOMPLETE} - reads included - which the frontend turns into a redirect to the
 * completion page.
 */
@Component
public class ProfileCompletionInterceptor implements HandlerInterceptor {

    /** Endpoints a half-finished account must still be able to call to get itself unstuck. */
    private static final Set<String> ALLOWED_PATHS = Set.of(
        "/api/auth/complete-profile",
        // Without this the frontend cannot even discover that the profile is incomplete.
        "/api/auth/me",
        "/api/auth/logout",
        "/api/auth/login",
        "/api/auth/login-link",
        "/api/auth/login-link/consume",
        "/api/auth/register",
        "/api/auth/registration-status",
        "/api/auth/registration-claim",
        "/api/auth/verification-challenge",
        "/api/auth/verification-confirm",
        "/api/auth/verification-reject",
        "/api/auth/verify-email",
        "/api/auth/resend-verification",
        "/api/auth/forgot-password",
        "/api/auth/reset-password");

    /** Mirrors {@code ErrorResponseDto}; hand-written because the payload is entirely constant. */
    private static final String PROFILE_INCOMPLETE_BODY =
        "{\"code\":\"PROFILE_INCOMPLETE\",\"message\":\"Please complete your profile before continuing.\"}";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws IOException {

        // Preflights carry no credentials and must never be answered with an error, or the browser
        // reports a CORS failure instead of the 403 the actual request would have produced.
        if (HttpMethod.OPTIONS.matches(request.getMethod())
            || ALLOWED_PATHS.contains(request.getRequestURI())) {
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
