package org.cthub.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    // POST /api/auth/login
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequestDto loginRequest, HttpServletRequest request) {
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
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // Kill the session
        }
        SecurityContextHolder.clearContext();
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