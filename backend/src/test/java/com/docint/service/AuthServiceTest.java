package com.docint.service;

import com.docint.dto.request.LoginRequest;
import com.docint.dto.request.RegisterRequest;
import com.docint.dto.response.AuthResponse;
import com.docint.entity.User;
import com.docint.exception.ConflictException;
import com.docint.repository.UserRepository;
import com.docint.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Mockito unit test for AuthService.
 *
 * AuthService depends on 5 collaborators (DB repo, password encoder, JWT provider,
 * auth manager, audit). We DON'T want a real database or real crypto here — we want
 * to test AuthService's LOGIC in isolation. So we replace each dependency with a @Mock
 * (a programmable fake) and tell it exactly how to behave for each scenario.
 *
 *   @Mock        → creates a fake of that type
 *   @InjectMocks → creates the real AuthService and injects the mocks into it
 *   when(...).thenReturn(...) → "when this method is called, return this" (stubbing)
 *   verify(...)  → "assert this method was actually called" (behavior verification)
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuditService auditService;

    @InjectMocks private AuthService authService;

    // ─────────────── register ───────────────

    @Test
    @DisplayName("register creates a PENDING, disabled user and audits it")
    void registerCreatesPendingUser() {
        var request = new RegisterRequest("new@docint.com", "password123", "New User");
        when(userRepository.existsByEmail("new@docint.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");

        String message = authService.register(request);

        assertThat(message).containsIgnoringCase("awaiting admin approval");

        // ArgumentCaptor lets us inspect the User object that was actually saved
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(User.Status.PENDING);
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-pw");   // stored the HASH, not the raw pw
        assertThat(saved.getRole()).isEqualTo(User.Role.USER);

        verify(auditService).log(eq("new@docint.com"), eq("REGISTER"), anyString(), anyString());
    }

    @Test
    @DisplayName("register rejects a duplicate email")
    void registerRejectsDuplicate() {
        when(userRepository.existsByEmail("dupe@docint.com")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("dupe@docint.com", "password123", "Dupe")))
                .isInstanceOf(ConflictException.class);

        // The user must NOT be saved when the email already exists
        verify(userRepository, never()).save(any());
    }

    // ─────────────── login ───────────────

    @Test
    @DisplayName("successful login returns tokens, sets lastLogin, and audits")
    void loginSuccess() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@docint.com")
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .tokenVersion(0)
                .build();
        when(userRepository.findByEmail("alice@docint.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken("alice@docint.com", 0)).thenReturn("access-tok");
        when(jwtTokenProvider.generateRefreshToken("alice@docint.com", 0)).thenReturn("refresh-tok");

        AuthResponse response = authService.login(new LoginRequest("alice@docint.com", "password123"));

        assertThat(response.token()).isEqualTo("access-tok");
        assertThat(response.refreshToken()).isEqualTo("refresh-tok");
        assertThat(response.role()).isEqualTo("USER");

        // authentication was actually attempted, lastLogin persisted, login audited
        verify(authenticationManager).authenticate(any());
        verify(userRepository).save(user);
        assertThat(user.getLastLogin()).isNotNull();
        verify(auditService).log(eq("alice@docint.com"), eq("LOGIN"), anyString(), any());
    }

    @Test
    @DisplayName("a PENDING account cannot log in and is never authenticated")
    void loginBlockedForPendingUser() {
        User pending = User.builder()
                .email("pending@docint.com")
                .status(User.Status.PENDING)
                .role(User.Role.USER)
                .build();
        when(userRepository.findByEmail("pending@docint.com")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("pending@docint.com", "password123")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("awaiting admin approval");

        // CRITICAL: we must short-circuit BEFORE checking the password
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtTokenProvider, never()).generateAccessToken(anyString(), anyInt());
    }

    @Test
    @DisplayName("logout-all bumps the user's token version, invalidating all tokens")
    void logoutAllBumpsTokenVersion() {
        User user = User.builder()
                .email("alice@docint.com")
                .tokenVersion(2)
                .build();
        when(userRepository.findByEmail("alice@docint.com")).thenReturn(Optional.of(user));

        authService.logoutAll("alice@docint.com");

        assertThat(user.getTokenVersion()).isEqualTo(3);   // incremented → old tokens now invalid
        verify(userRepository).save(user);
    }
}
