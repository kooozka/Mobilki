package com.example.demo.dispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRouteRequest {
    private LocalDate routeDate;
    private Long driverId;
    private Long vehicleId;
    private List<Long> orderIds;
}
