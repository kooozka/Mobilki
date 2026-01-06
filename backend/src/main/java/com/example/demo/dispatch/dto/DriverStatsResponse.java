package com.example.demo.dispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatsResponse {
    private long totalOrders;
    private long completedOrders;
    private long activeOrders;
    private long totalRoutes;
    private long completedRoutes;
    private long activeRoutes;
    private double totalDistanceKm;
}
