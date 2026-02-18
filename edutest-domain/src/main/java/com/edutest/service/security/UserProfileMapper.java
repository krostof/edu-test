package com.edutest.service.security;

import com.edutest.api.model.UserProfile;
import com.edutest.api.model.UserRole;
import com.edutest.api.model.UserSecurity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component("securityUserProfileMapper")
public class UserProfileMapper {

    public UserProfile toUserProfile(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new UserProfile()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .role(toUserRole(entity.getRole()))
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt() != null ?
                    entity.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
    }

    public UserProfile toUserProfile(UserSecurity userSecurity) {
        if (userSecurity == null) {
            return null;
        }

        return new UserProfile()
                .id(userSecurity.getId())
                .username(userSecurity.getUsername())
                .email(userSecurity.getEmail())
                .firstName(userSecurity.getFirstName())
                .lastName(userSecurity.getLastName())
                .role(userSecurity.getRole())
                .isActive(userSecurity.getIsActive())
                .createdAt(userSecurity.getCreatedAt());
    }

    public UserSecurity toUserSecurity(UserEntity entity) {
        if (entity == null) {
            return null;
        }

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
                    entity.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
                .updatedAt(entity.getUpdatedAt() != null ?
                    entity.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    }

    public UserEntity toUserEntity(UserProfile userProfile) {
        if (userProfile == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(userProfile.getUsername());
        entity.setEmail(userProfile.getEmail());
        entity.setFirstName(userProfile.getFirstName());
        entity.setLastName(userProfile.getLastName());
        entity.setRole(toUserEntityRole(userProfile.getRole()));
        entity.setIsActive(userProfile.getIsActive());

        // Set ID manually if it exists (for updates)
        if (userProfile.getId() != null) {
            entity.setId(userProfile.getId());
        }

        return entity;
    }

    private UserRole toUserRole(UserEntityRole entityRole) {
        if (entityRole == null) {
            return null;
        }
        return switch (entityRole) {
            case STUDENT -> UserRole.STUDENT;
            case TEACHER -> UserRole.TEACHER;
            case ADMIN -> UserRole.ADMIN;
        };
    }

    private UserEntityRole toUserEntityRole(UserRole userRole) {
        if (userRole == null) {
            return null;
        }
        return switch (userRole) {
            case STUDENT -> UserEntityRole.STUDENT;
            case TEACHER -> UserEntityRole.TEACHER;
            case ADMIN -> UserEntityRole.ADMIN;
        };
    }
}
