package com.edutest.persistance.entity.user;

import lombok.Getter;

@Getter
public enum UserRole {
    STUDENT("Student"),
    TEACHER("Teacher"),
    ADMIN("Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public boolean hasAdminPrivileges() {
        return this == ADMIN;
    }

    public boolean canCreateTests() {
        return this == TEACHER || this == ADMIN;
    }

    public boolean canManageUsers() {
        return this == ADMIN;
    }
}
