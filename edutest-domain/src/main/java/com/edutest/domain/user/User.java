package com.edutest.domain.user;

import com.edutest.domain.common.DomainEntity;
import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
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

    @Enumerated(EnumType.STRING)
    private UserEntityRole role;

    @Builder.Default
    private Boolean isActive = true;

    private String studentNumber;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isStudent() {
        return UserEntityRole.STUDENT.equals(role);
    }

    public boolean isTeacher() {
        return UserEntityRole.TEACHER.equals(role);
    }

    public boolean isAdmin() {
        return UserEntityRole.ADMIN.equals(role);
    }
}
