package org.cthub.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.User;
import org.cthub.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("🌱 No users found. Seeding initial Admin account...");

            User admin = User.builder()
                .email("admin@challengeteamhub.org")
                .password(passwordEncoder.encode("admin123")) // Change this immediately!
                .role(User.Role.ADMIN)
                .build();

            userRepository.save(admin);
            log.info("✅ Admin created: admin@cthub.org / admin123");
        }
    }
}