package com.edutest.util;

import com.edutest.api.model.UserProfile;
import com.edutest.domain.user.User;
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
                .role(userProfile.getRole())
                .build();
    }
}
