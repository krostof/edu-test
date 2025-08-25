package com.edutest.util;

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
                .role(UserEntityRole.STUDENT)
                .build();
    }

}
