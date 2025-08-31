package com.edutest.persistance.entity.user;

import com.edutest.persistance.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserEntityRole role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "student_number", length = 20)
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
