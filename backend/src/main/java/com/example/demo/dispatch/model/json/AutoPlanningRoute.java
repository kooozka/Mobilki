package com.example.demo.dispatch.model.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutoPlanningRoute {
    private Long driverId;

    private Long vehicleId;

    private List<Long> orderIdsOrdered;

    private Double totalDistance;

    private Integer estimatedTimeMinutes;
}
