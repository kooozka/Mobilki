package com.example.demo.security.model;

public enum UserRole {
    CLIENT("Klient"),
    DISPATCH_MANAGER("Kierownik Spedycji"),
    DRIVER("Kierowca"),
    ADMIN("Administrator Systemu");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
