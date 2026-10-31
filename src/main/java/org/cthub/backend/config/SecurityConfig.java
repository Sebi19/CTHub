package org.cthub.backend.config;

import org.cthub.backend.auth.AuthCookies;
import org.cthub.backend.auth.session.AuthSessionService;
import org.cthub.backend.auth.session.SessionAuthenticationFilter;
import org.cthub.backend.auth.session.SessionIssuer;
import org.cthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Authentication is session based: a 90-day {@code AuthSession} with a rotating refresh cookie,
 * cached per browser in a short-lived HTTP session (see {@link SessionAuthenticationFilter}).
 * Logins happen in the auth controllers, never through Spring Security's form/basic login.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Value("${cors.allowed-patterns:}")
    private String corsAllowedPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthSessionService sessionService,
                                                   SessionIssuer issuer, AuthCookies cookies,
                                                   UserRepository userRepository) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Auth cookies are SameSite=Lax and every mutation is a non-simple JSON request, which
            // browsers only send cross-origin after a CORS preflight against the allow-list below.
            .csrf(csrf -> csrf.disable())
            .addFilterAfter(new SessionAuthenticationFilter(sessionService, issuer, cookies, userRepository),
                SecurityContextHolderFilter.class)
            // Authorization happens per endpoint with @PreAuthorize.
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Defined so Spring Boot doesn't auto-configure an in-memory user with a generated password. */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepo) {
        return email -> userRepo.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .toList();
        configuration.setAllowedOrigins(origins);

        if (corsAllowedPatterns != null && !corsAllowedPatterns.isBlank()) {
            List<String> patterns = Arrays.stream(corsAllowedPatterns.split(","))
                .map(String::trim)
                .toList();
            configuration.setAllowedOriginPatterns(patterns);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
