package com.edutest.util;

import com.edutest.api.model.UserProfile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserProfileMapper {

    public com.edutest.domain.user.User toUser(UserProfile userProfile) {
        if (userProfile == null) {
            return null;
        }

        Set<com.edutest.domain.user.UserRole> roles = new HashSet<>();

        // Support both roles list and single role for backwards compatibility
        if (userProfile.getRoles() != null && !userProfile.getRoles().isEmpty()) {
            roles = userProfile.getRoles().stream()
                    .map(this::mapToDomainRole)
                    .collect(Collectors.toSet());
        } else if (userProfile.getRole() != null) {
            roles.add(mapToDomainRole(userProfile.getRole()));
        }

        return com.edutest.domain.user.User.builder()
                .username(userProfile.getUsername())
                .email(userProfile.getEmail())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .roles(roles)
                .build();
    }

    private com.edutest.domain.user.UserRole mapToDomainRole(com.edutest.api.model.UserRole apiRole) {
        if (apiRole == null) {
            return null;
        }

        return switch (apiRole) {
            case STUDENT -> com.edutest.domain.user.UserRole.STUDENT;
            case TEACHER -> com.edutest.domain.user.UserRole.TEACHER;
            case ADMIN -> com.edutest.domain.user.UserRole.ADMIN;
        };
    }
}
