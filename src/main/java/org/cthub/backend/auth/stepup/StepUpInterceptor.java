package org.cthub.backend.auth.stepup;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.auth.session.AuthSessionService;
import org.cthub.backend.auth.session.SessionIssuer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Enforces {@link RequiresStepUp}. Unauthenticated requests are left to {@code @PreAuthorize}. */
@Component
@RequiredArgsConstructor
public class StepUpInterceptor implements HandlerInterceptor {

    public static final Duration MAX_AGE = Duration.ofMinutes(10);

    /** Mirrors {@code ErrorResponseDto}. */
    private static final String STEP_UP_REQUIRED_BODY =
        "{\"code\":\"STEP_UP_REQUIRED\",\"message\":\"Please confirm it's you before making this change.\"}";

    private final AuthSessionService sessionService;
    private final Clock clock;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())
            || !(handler instanceof HandlerMethod method) || !requiresStepUp(method)) {
            return true;
        }
        Optional<UUID> sessionId = SessionIssuer.currentSessionId(request);
        if (sessionId.isEmpty()) {
            return true;
        }
        Optional<Instant> steppedUpAt = sessionService.stepUpAt(sessionId.get());
        if (steppedUpAt.isPresent() && steppedUpAt.get().plus(MAX_AGE).isAfter(clock.instant())) {
            return true;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(STEP_UP_REQUIRED_BODY);
        return false;
    }

    private static boolean requiresStepUp(HandlerMethod method) {
        return method.hasMethodAnnotation(RequiresStepUp.class)
            || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), RequiresStepUp.class);
    }
}
