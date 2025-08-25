package com.edutest.util;

import com.edutest.api.model.UserProfile;
import com.edutest.api.model.UserRole;
import com.edutest.domain.user.User;
import com.edutest.persistance.entity.user.UserEntityRole;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public User toUser(UserProfile userProfile) {
        if (userProfile == null) {
            return null;
        }
        
        return User.builder()
                .username(userProfile.getUsername())
                .email(userProfile.getEmail())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .role(mapToEntityRole(userProfile.getRole()))
                .build();
    }
    
    private UserEntityRole mapToEntityRole(UserRole userRole) {
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
