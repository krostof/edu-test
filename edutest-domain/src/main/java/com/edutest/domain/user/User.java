package com.edutest.domain.user;

import com.edutest.domain.common.DomainEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends DomainEntity {

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private UserRole role;

    @Builder.Default
    private Boolean isActive = true;

    private String studentNumber;

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
