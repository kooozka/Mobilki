package com.example.optimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RunningOptimization {

    private Long id;

    private OptimizationStatus status;

    private Set<Driver> drivers;

    private Set<Vehicle> vehicles;

    // posortowana lista, aby znać kolejność dystansu w macierzy odległości
    private List<Order> orders;

    private double[][] distanceMatrix;

    private double[][] durationMatrix;

    private List<OptimizedRoute> routes = null;

}
