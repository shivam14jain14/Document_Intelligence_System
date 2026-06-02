package com.docint.config;

import com.docint.entity.User;
import com.docint.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN account on first startup so the system is usable
 * (self-registered users are PENDING and need an admin to approve them).
 * Credentials are configurable; change them after first login in production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        boolean anyAdmin = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() == User.Role.ADMIN);
        if (anyAdmin) return;

        String email = "admin@docint.com";
        if (userRepository.existsByEmail(email)) return;

        User admin = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .role(User.Role.ADMIN)
                .status(User.Status.ACTIVE)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.warn("=================================================================");
        log.warn(" Seeded default ADMIN account:  admin@docint.com / admin123");
        log.warn(" Change this password after first login.");
        log.warn("=================================================================");
    }
}
