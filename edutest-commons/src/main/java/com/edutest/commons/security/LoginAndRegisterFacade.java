package com.edutest.commons.security;

import com.edutest.api.model.UserSecurity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole() != null ? request.getRole() : UserEntityRole.STUDENT)
                .isActive(true)
                .studentNumber(request.getStudentNumber())
                .build();

        UserEntity savedUser = userRepository.save(user);
        return mapToUserSecurity(savedUser);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private UserSecurity mapToUserSecurity(UserEntity entity) {
        return new UserSecurity()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .role(toUserRole(entity.getRole()))
                .isActive(entity.getIsActive())
                .studentNumber(entity.getStudentNumber())
                .createdAt(entity.getCreatedAt() != null ? 
                    entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(entity.getUpdatedAt() != null ? 
                    entity.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null);
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
        private UserEntityRole role;
        private String studentNumber;
    }
}