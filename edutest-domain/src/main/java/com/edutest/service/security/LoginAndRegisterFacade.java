package com.edutest.service.security;

import com.edutest.api.model.UserSecurity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginAndRegisterFacade {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<UserSecurity> findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserSecurity);
    }

    public Optional<UserSecurity> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToUserSecurity);
    }

    public UserSecurity registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        // Support both single role (backwards compatible) and multiple roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            request.getRoles().forEach(user::addRole);
        } else if (request.getRole() != null) {
            user.addRole(request.getRole());
        } else {
            user.addRole(UserEntityRole.STUDENT);
        }
        user.setIsActive(true);
        user.setStudentNumber(request.getStudentNumber());

        UserEntity savedUser = userRepository.save(user);
        return mapToUserSecurity(savedUser);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private UserSecurity mapToUserSecurity(UserEntity entity) {
        // Get the primary role for backwards compatibility with API
        UserEntityRole primaryRole = entity.getRole();

        UserSecurity security = new UserSecurity()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .role(toUserRole(primaryRole))
                .isActive(entity.getIsActive())
                .studentNumber(entity.getStudentNumber())
                .createdAt(entity.getCreatedAt() != null ?
                    entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(entity.getUpdatedAt() != null ?
                    entity.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null);

        // Store all roles as a list for multi-role support
        if (entity.getRoles() != null) {
            List<com.edutest.api.model.UserRole> rolesList = entity.getRoles().stream()
                    .map(this::toUserRole)
                    .collect(Collectors.toList());
            security.setRoles(rolesList);
        }

        return security;
    }

    private com.edutest.api.model.UserRole toUserRole(UserEntityRole entityRole) {
        if (entityRole == null) {
            return null;
        }
        return switch (entityRole) {
            case STUDENT -> com.edutest.api.model.UserRole.STUDENT;
            case TEACHER -> com.edutest.api.model.UserRole.TEACHER;
            case ADMIN -> com.edutest.api.model.UserRole.ADMIN;
        };
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        @Deprecated
        private UserEntityRole role;
        private Set<UserEntityRole> roles;
        private String studentNumber;
    }
}
