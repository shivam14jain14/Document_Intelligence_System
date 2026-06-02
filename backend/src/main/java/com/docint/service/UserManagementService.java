package com.docint.service;

import com.docint.dto.request.CreateUserRequest;
import com.docint.dto.response.UserDTO;
import com.docint.entity.User;
import com.docint.exception.ConflictException;
import com.docint.exception.ResourceNotFoundException;
import com.docint.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UserDTO> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(UserDTO::from).toList();
    }

    @Transactional
    public UserDTO createUser(CreateUserRequest req, String adminEmail) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered: " + req.email());
        }
        User.Role role = "ADMIN".equalsIgnoreCase(req.role()) ? User.Role.ADMIN : User.Role.USER;
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role(role)
                .status(User.Status.ACTIVE)   // admin-created users are active immediately
                .enabled(true)
                .build();
        User saved = userRepository.save(user);
        auditService.log(adminEmail, "CREATE_USER", req.email(), "role=" + role);
        return UserDTO.from(saved);
    }

    @Transactional
    public UserDTO approve(UUID userId, String adminEmail) {
        User user = getUser(userId);
        user.setStatus(User.Status.ACTIVE);
        user.setEnabled(true);
        auditService.log(adminEmail, "APPROVE_USER", user.getEmail(), null);
        return UserDTO.from(userRepository.save(user));
    }

    @Transactional
    public UserDTO setStatus(UUID userId, String status, String adminEmail) {
        User user = getUser(userId);
        User.Status newStatus = User.Status.valueOf(status.toUpperCase());
        user.setStatus(newStatus);
        user.setEnabled(newStatus == User.Status.ACTIVE);
        auditService.log(adminEmail, "SET_USER_STATUS", user.getEmail(), "status=" + newStatus);
        return UserDTO.from(userRepository.save(user));
    }

    @Transactional
    public UserDTO setRole(UUID userId, String role, String adminEmail) {
        User user = getUser(userId);
        user.setRole(User.Role.valueOf(role.toUpperCase()));
        auditService.log(adminEmail, "SET_USER_ROLE", user.getEmail(), "role=" + role);
        return UserDTO.from(userRepository.save(user));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
