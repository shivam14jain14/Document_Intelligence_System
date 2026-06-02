package com.docint.dto.response;

import java.util.UUID;

public record AuthResponse(
        String token,
        String refreshToken,
        String tokenType,
        UUID userId,
        String email,
        String fullName,
        String role,
        boolean onboardingCompleted
) {
    public static AuthResponse of(String token, String refreshToken, UUID userId,
                                  String email, String fullName, String role,
                                  boolean onboardingCompleted) {
        return new AuthResponse(token, refreshToken, "Bearer", userId, email, fullName, role, onboardingCompleted);
    }
}
