package com.example.entities;

public enum UserRole {
    USER("User"),
    ADMIN("Administrator"),
    LIBRARIAN("Librarian"),
    RESTRICTED_ADMIN("Restricted Admin");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAdmin() {
        return this == ADMIN || this == RESTRICTED_ADMIN;
    }

    public boolean isStaff() {
        return this == LIBRARIAN || this == ADMIN || this == RESTRICTED_ADMIN;
    }
}