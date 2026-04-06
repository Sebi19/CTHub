package org.cthub.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.User;
import org.cthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-password:#{null}}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("🌱 No users found. Seeding initial Admin account...");

            String initialPassword = adminPassword != null ? adminPassword : UUID.randomUUID().toString();

            User admin = User.builder()
                .email("admin@challengeteamhub.org")
                .password(passwordEncoder.encode(initialPassword))
                .role(User.Role.ADMIN)
                .build();

            userRepository.save(admin);
            log.info("✅ Admin created: admin@challengeteamhub.org");

            if (adminPassword == null) {
                log.warn("⚠️ No admin password configured! Generated random password: {}", initialPassword);
            }
        }
    }
}