package com.edutest.util;

import com.edutest.api.model.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public com.edutest.domain.user.User toUser(UserProfile userProfile) {
        if (userProfile == null) {
            return null;
        }
        
        return com.edutest.domain.user.User.builder()
                .username(userProfile.getUsername())
                .email(userProfile.getEmail())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .role(mapToDomainRole(userProfile.getRole()))
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
