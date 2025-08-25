package com.edutest.util;

import com.edutest.api.model.UserProfile;
import com.edutest.api.model.UserRole;
import com.edutest.domain.user.User;
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
                .role(entity.getRole())
                .build();
    }
    
    public UserProfile toUserProfile(UserEntity entity) {
        UserProfile userProfile = new UserProfile();
        userProfile.setId(entity.getId());
        userProfile.setUsername(entity.getUsername());
        userProfile.setEmail(entity.getEmail());
        userProfile.setFirstName(entity.getFirstName());
        userProfile.setLastName(entity.getLastName());
        userProfile.setStudentNumber(entity.getStudentNumber());
        userProfile.setActive(entity.getIsActive());
        return userProfile;
    }

}
