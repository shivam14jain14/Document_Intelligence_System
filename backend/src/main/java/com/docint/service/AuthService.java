package com.docint.service;

import com.docint.dto.request.LoginRequest;
import com.docint.dto.request.RegisterRequest;
import com.docint.dto.response.AuthResponse;
import com.docint.entity.User;
import com.docint.exception.ConflictException;
import com.docint.repository.UserRepository;
import com.docint.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    /**
     * Self-registration creates an active user immediately.
     * It does not return a token; the user still signs in afterward.
     */
    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .enabled(true)
                .build();

        userRepository.save(user);
        auditService.log(request.email(), "REGISTER", request.email(), "Self-registration activated immediately");

        return "Registration successful. You can sign in right away.";
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid email or password"));

        if (user.getStatus() == User.Status.DISABLED) {
            throw new DisabledException("Your account has been disabled. Contact an administrator.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        auditService.log(user.getEmail(), "LOGIN", user.getEmail(), null);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)
                || !"refresh".equals(jwtTokenProvider.extractType(refreshToken))) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid refresh token");
        }
        String email = jwtTokenProvider.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid refresh token"));

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new DisabledException("Account is not active.");
        }
        if (jwtTokenProvider.extractVersion(refreshToken) != user.getTokenVersion()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Refresh token has been revoked");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public void logoutAll(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
            auditService.log(email, "LOGOUT_ALL", email, "All sessions invalidated");
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        String access = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getTokenVersion());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getTokenVersion());
        return AuthResponse.of(access, refresh, user.getId(), user.getEmail(),
                user.getFullName(), user.getRole().name(), user.isOnboardingCompleted());
    }
}
