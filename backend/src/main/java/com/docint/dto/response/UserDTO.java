package com.docint.dto.response;

import com.docint.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String email,
        String fullName,
        String role,
        String status,
        LocalDateTime lastLogin,
        LocalDateTime createdAt
) {
    public static UserDTO from(User u) {
        return new UserDTO(
                u.getId(), u.getEmail(), u.getFullName(),
                u.getRole().name(), u.getStatus().name(), u.getLastLogin(), u.getCreatedAt());
    }
}
