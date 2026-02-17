package com.edutest.util;

import com.edutest.api.model.CreateStudentRequest;
import com.edutest.api.model.CreateTeacherRequest;
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
        userProfile.setRole(mapToApiUserRole(entity.getRole()));
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
        return UserEntity.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .studentNumber(request.getStudentNumber())
                .role(UserEntityRole.STUDENT)
                .isActive(true)
                .build();
    }

    public UserEntity toTeacherEntity(CreateTeacherRequest request, String encodedPassword) {
        return UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserEntityRole.TEACHER)
                .isActive(true)
                .build();
    }

}
