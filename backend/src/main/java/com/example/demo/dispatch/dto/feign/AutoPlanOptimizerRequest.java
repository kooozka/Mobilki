package com.example.demo.dispatch.dto.feign;

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
public class AutoPlanOptimizerRequest {
    private Long planningId;

    private Set<PlanningDriverTO> drivers;

    private Set<PlanningVehicleTO> vehicles;

    // posortowana lista, aby znać kolejność dystansu w macierzy odległości
    private List<PlanningOrderTO> orders;

    private double[][] distanceMatrix;

    private double[][] durationMatrix;
}
