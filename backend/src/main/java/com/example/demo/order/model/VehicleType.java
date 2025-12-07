package com.example.demo.order.model;

public enum VehicleType {
    SMALL_VAN("Mały van", 1500),
    MEDIUM_TRUCK("Średnia ciężarówka", 5000),
    LARGE_TRUCK("Duża ciężarówka", 12000),
    SEMI_TRUCK("Naczepa", 25000);

    private final String displayName;
    private final double maxWeight; // w tonach

    VehicleType(String displayName, double maxWeight) {
        this.displayName = displayName;
        this.maxWeight = maxWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMaxWeight() {
        return maxWeight;
    }
}
