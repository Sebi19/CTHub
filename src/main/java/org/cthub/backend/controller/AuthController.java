package org.cthub.backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.auth.LoginRequestDto;
import org.cthub.backend.dto.auth.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final RememberMeServices rememberMeServices;

    // POST /api/auth/login
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserDto> login(
        @RequestBody LoginRequestDto loginRequest,
        HttpServletRequest request,
        HttpServletResponse response) {

        // 1. Authenticate the user (checks DB & password)
        Authentication authenticationRequest =
            UsernamePasswordAuthenticationToken.unauthenticated(loginRequest.getEmail(), loginRequest.getPassword());

        Authentication authenticationResponse =
            this.authenticationManager.authenticate(authenticationRequest);

        // 2. Create the Session manually
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticationResponse);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        rememberMeServices.loginSuccess(request, response, authenticationResponse);

        // 3. Return User Info (No redirect!)
        org.cthub.backend.model.User user = (org.cthub.backend.model.User) authenticationResponse.getPrincipal(); // Assuming your User implements UserDetails

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .role(user.getRole().name())
            .build());
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // Kill the session
        }
        SecurityContextHolder.clearContext();
        Cookie rememberMeCookie = new Cookie("remember-me", null);
        rememberMeCookie.setMaxAge(0); // This tells the browser to delete it immediately
        rememberMeCookie.setPath("/"); // Must match the path of the original cookie
        rememberMeCookie.setHttpOnly(true);
        response.addCookie(rememberMeCookie);

        // 4. Nuke the JSESSIONID cookie (Good practice)
        Cookie jsessionCookie = new Cookie("JSESSIONID", null);
        jsessionCookie.setMaxAge(0);
        jsessionCookie.setPath("/");
        jsessionCookie.setHttpOnly(true);
        response.addCookie(jsessionCookie);
        return ResponseEntity.ok("Logged out successfully");
    }

    // GET /api/auth/me (Check current session)
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof org.cthub.backend.model.User user) {
            return ResponseEntity.ok(UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}