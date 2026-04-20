package com.edutest.util;

import com.edutest.api.model.CreateStudentRequest;
import com.edutest.api.model.CreateTeacherRequest;
import com.edutest.api.model.UserProfile;
import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRole;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toUser(UserEntity entity) {
        Set<UserRole> roles = mapToUserRoles(entity.getRoles());

        User user = User.builder()
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .roles(roles)
                .isActive(entity.getIsActive())
                .studentNumber(entity.getStudentNumber())
                .build();
        user.setId(entity.getId());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        return user;
    }

    private Set<UserRole> mapToUserRoles(Set<UserEntityRole> entityRoles) {
        if (entityRoles == null || entityRoles.isEmpty()) {
            return new HashSet<>();
        }
        return entityRoles.stream()
                .map(this::mapToUserRole)
                .collect(Collectors.toSet());
    }

    private UserRole mapToUserRole(UserEntityRole entityRole) {
        if (entityRole == null) {
            return null;
        }

        return switch (entityRole) {
            case STUDENT -> UserRole.STUDENT;
            case TEACHER -> UserRole.TEACHER;
            case ADMIN -> UserRole.ADMIN;
        };
    }

    public UserProfile toUserProfile(UserEntity entity) {
        UserProfile userProfile = new UserProfile();
        userProfile.setId(entity.getId());
        userProfile.setUsername(entity.getUsername());
        userProfile.setEmail(entity.getEmail());
        userProfile.setFirstName(entity.getFirstName());
        userProfile.setLastName(entity.getLastName());
        userProfile.setRole(mapToApiUserRole(entity.getRole()));

        // Set all roles
        if (entity.getRoles() != null && !entity.getRoles().isEmpty()) {
            userProfile.setRoles(entity.getRoles().stream()
                    .map(this::mapToApiUserRole)
                    .collect(Collectors.toList()));
        }

        userProfile.setIsActive(entity.getIsActive());
        return userProfile;
    }

    private com.edutest.api.model.UserRole mapToApiUserRole(UserEntityRole entityRole) {
        if (entityRole == null) {
            return null;
        }

        return switch (entityRole) {
            case STUDENT -> com.edutest.api.model.UserRole.STUDENT;
            case TEACHER -> com.edutest.api.model.UserRole.TEACHER;
            case ADMIN -> com.edutest.api.model.UserRole.ADMIN;
        };
    }

    public UserEntityRole toEntityRole(com.edutest.api.model.UserRole role) {
        if (role == null) {
            return null;
        }

        return switch (role) {
            case STUDENT -> UserEntityRole.STUDENT;
            case TEACHER -> UserEntityRole.TEACHER;
            case ADMIN -> UserEntityRole.ADMIN;
        };
    }

    public UserEntity toStudentEntity(CreateStudentRequest request, String encodedPassword) {
        UserEntity entity = new UserEntity();
        entity.setEmail(request.getEmail());
        entity.setPassword(encodedPassword);
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setStudentNumber(request.getStudentNumber());
        entity.addRole(UserEntityRole.STUDENT);
        entity.setIsActive(true);
        return entity;
    }

    public UserEntity toTeacherEntity(CreateTeacherRequest request, String encodedPassword) {
        UserEntity entity = new UserEntity();
        entity.setUsername(request.getUsername());
        entity.setEmail(request.getEmail());
        entity.setPassword(encodedPassword);
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.addRole(UserEntityRole.TEACHER);
        entity.setIsActive(true);
        return entity;
    }

}
