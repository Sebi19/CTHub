package org.cthub.backend.config;

import org.cthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Value("${app.security.remember-me-key}")
    private String rememberMeKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RememberMeServices rememberMeServices) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 1. Disable CSRF (Common for REST APIs, crucial for Postman testing)
            .csrf(csrf -> csrf.disable())
            .rememberMe(rm -> rm.rememberMeServices(rememberMeServices))
            // 2. Allow everything
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepo) {
        return email -> userRepo.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Bean
    public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .toList();

        // 1. Enter your exact Frontend URL here (no trailing slash!)
        configuration.setAllowedOrigins(origins);

        // 2. Allow all standard methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. Allow headers (needed for sending JSON, Auth tokens, etc.)
        configuration.setAllowedHeaders(List.of("*"));

        // 4. Allow credentials (cookies/auth headers)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        // Using Spring Security 6's TokenBasedRememberMeServices
        TokenBasedRememberMeServices.RememberMeTokenAlgorithm encodingAlgorithm =
            TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256;

        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices(
            rememberMeKey,
            userDetailsService,
            encodingAlgorithm
        );

        // Set cookie validity to 90 days (in seconds)
        rememberMe.setTokenValiditySeconds(60 * 60 * 24 * 90);
        rememberMe.setCookieName("remember-me");

        rememberMe.setAlwaysRemember(true);

        return rememberMe;
    }
}