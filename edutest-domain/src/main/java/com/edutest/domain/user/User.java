package com.edutest.domain.user;

import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.user.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    private String username;

    private String email;


    private String firstName;

    private String lastName;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Builder.Default
    private Boolean isActive = true;

    private String studentNumber;

    private String employeeId;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isStudent() {
        return UserRole.STUDENT.equals(role);
    }

    public boolean isTeacher() {
        return UserRole.TEACHER.equals(role);
    }

    public boolean isAdmin() {
        return UserRole.ADMIN.equals(role);
    }
}
