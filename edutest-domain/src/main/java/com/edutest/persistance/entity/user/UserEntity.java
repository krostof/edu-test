package com.edutest.persistance.entity.user;

import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<UserEntityRole> roles = new HashSet<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "student_number", length = 20)
    private String studentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_group_id")
    private StudentGroupEntity studentGroup;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(UserEntityRole role) {
        return roles != null && roles.contains(role);
    }

    public boolean isStudent() {
        return hasRole(UserEntityRole.STUDENT);
    }

    public boolean isTeacher() {
        return hasRole(UserEntityRole.TEACHER);
    }

    public boolean isAdmin() {
        return hasRole(UserEntityRole.ADMIN);
    }

    /**
     * Helper method to add a role to the user.
     */
    public void addRole(UserEntityRole role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    /**
     * Helper method to remove a role from the user.
     */
    public void removeRole(UserEntityRole role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Backwards-compatible getter that returns the primary role.
     * Returns the first role found, prioritizing ADMIN > TEACHER > STUDENT.
     * @deprecated Use getRoles() instead for multi-role support.
     */
    @Deprecated
    public UserEntityRole getRole() {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        if (roles.contains(UserEntityRole.ADMIN)) {
            return UserEntityRole.ADMIN;
        }
        if (roles.contains(UserEntityRole.TEACHER)) {
            return UserEntityRole.TEACHER;
        }
        return UserEntityRole.STUDENT;
    }

    /**
     * Backwards-compatible setter that sets a single role.
     * @deprecated Use addRole() instead for multi-role support.
     */
    @Deprecated
    public void setRole(UserEntityRole role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.clear();
        if (role != null) {
            roles.add(role);
        }
    }
}
