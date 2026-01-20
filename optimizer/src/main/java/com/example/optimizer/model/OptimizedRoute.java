package com.example.optimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OptimizedRoute {
    private Long driverId;

    private Long vehicleId;

    private List<Long> orderIdsOrdered;

    private Double totalDistance;

    private Integer estimatedTimeMinutes;
}
