package com.edutest.util;

import com.edutest.api.model.UserProfile;
import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRole;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toUser(UserEntity entity) {
        return User.builder()
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .role(mapToUserRole(entity.getRole()))
                .build();
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
        userProfile.setIsActive(entity.getIsActive());
        return userProfile;
    }

}
