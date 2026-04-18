package com.example.entities;

import java.io.Serializable;

public enum UserRole implements Serializable {
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