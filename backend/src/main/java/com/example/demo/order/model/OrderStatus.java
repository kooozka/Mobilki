package com.example.demo.order.model;

public enum OrderStatus {
    PENDING("Oczekuje"),
    CONFIRMED("Potwierdzone"),
    ASSIGNED("Przydzielone do kierowcy"),
    IN_PROGRESS("W realizacji"),
    COMPLETED("Zakończone"),
    CANCELLED("Anulowane");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
