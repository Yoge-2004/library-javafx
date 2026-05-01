package com.example.entities;

public enum UserRole {
    USER("User"),
    ADMIN("Administrator"),
    LIBRARIAN("Librarian");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isStaff() {
        return this == LIBRARIAN || this == ADMIN;
    }
}