package com.example.optimizer.dto;

import com.example.optimizer.model.Driver;
import com.example.optimizer.model.Order;
import com.example.optimizer.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizerRequest {
    private Long planningId;

    private Set<Driver> drivers;

    private Set<Vehicle> vehicles;

    // posortowana lista, aby znać kolejność dystansu w macierzy odległości
    private List<Order> orders;

    private double[][] distanceMatrix;

    private double[][] durationMatrix;
}
