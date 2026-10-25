package org.cthub.backend.config;

import org.cthub.backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileCompletionInterceptorTest {

    private final ProfileCompletionInterceptor interceptor = new ProfileCompletionInterceptor();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private User incompleteUser() {
        return User.builder().id(1L).email("pending@example.org").enabled(true).build();
    }

    private User completeUser() {
        return User.builder().id(2L).email("done@example.org").enabled(true)
            .firstName("Ada").lastName("Lovelace").profileCompletedAt(Instant.now()).build();
    }

    private boolean preHandle(String method, String uri) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    /** A name is mandatory, so an unfinished account may not even read application data. */
    @Test
    void blocksReadsFromAnIncompleteProfile() throws IOException {
        authenticate(incompleteUser());

        assertThat(preHandle("GET", "/api/teams")).isFalse();
        assertThat(preHandle("POST", "/api/teams")).isFalse();
    }

    @Test
    void respondsWithTheProfileIncompleteCode() throws IOException {
        authenticate(incompleteUser());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/teams");
        request.setRequestURI("/api/teams");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PROFILE_INCOMPLETE");
    }

    /** Without these the account could never get itself unstuck. */
    @Test
    void allowsTheEscapeHatches() throws IOException {
        authenticate(incompleteUser());

        assertThat(preHandle("GET", "/api/auth/me")).isTrue();
        assertThat(preHandle("POST", "/api/auth/complete-profile")).isTrue();
        assertThat(preHandle("POST", "/api/auth/logout")).isTrue();
    }

    /** A rejected preflight surfaces as a CORS failure rather than the 403 that was intended. */
    @Test
    void neverBlocksPreflightRequests() throws IOException {
        authenticate(incompleteUser());

        assertThat(preHandle("OPTIONS", "/api/teams")).isTrue();
    }

    @Test
    void leavesCompletedProfilesAlone() throws IOException {
        authenticate(completeUser());

        assertThat(preHandle("GET", "/api/teams")).isTrue();
        assertThat(preHandle("POST", "/api/teams")).isTrue();
    }

    @Test
    void leavesAnonymousRequestsToSpringSecurity() throws IOException {
        assertThat(preHandle("GET", "/api/teams")).isTrue();
    }
}
