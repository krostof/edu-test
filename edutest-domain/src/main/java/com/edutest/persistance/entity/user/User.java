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
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "student_number", length = 20)
    private String studentNumber;

    @Column(name = "employee_id", length = 20)
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
