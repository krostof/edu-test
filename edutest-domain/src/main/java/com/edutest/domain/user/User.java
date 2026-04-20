package com.edutest.domain.user;

import com.edutest.domain.common.DomainEntity;
import com.edutest.domain.group.StudentGroup;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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

    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @Builder.Default
    private Boolean isActive = true;

    private String studentNumber;

    private StudentGroup studentGroup;

    public boolean hasGroup() {
        return studentGroup != null;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }

    public boolean isStudent() {
        return hasRole(UserRole.STUDENT);
    }

    public boolean isTeacher() {
        return hasRole(UserRole.TEACHER);
    }

    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    public void addRole(UserRole role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    public void removeRole(UserRole role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Backwards-compatible getter that returns the primary role.
     * @deprecated Use getRoles() instead for multi-role support.
     */
    @Deprecated
    public UserRole getRole() {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        if (roles.contains(UserRole.ADMIN)) {
            return UserRole.ADMIN;
        }
        if (roles.contains(UserRole.TEACHER)) {
            return UserRole.TEACHER;
        }
        return UserRole.STUDENT;
    }

    /**
     * Backwards-compatible setter that sets a single role.
     * @deprecated Use addRole() instead for multi-role support.
     */
    @Deprecated
    public void setRole(UserRole role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.clear();
        if (role != null) {
            roles.add(role);
        }
    }
}
