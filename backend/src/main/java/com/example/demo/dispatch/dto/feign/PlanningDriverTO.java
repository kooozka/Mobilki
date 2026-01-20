package com.example.demo.dispatch.dto.feign;

import com.example.demo.order.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningDriverTO {
    private Long id;

    private Set<VehicleType> licences;

    private LocalTime workStart;

    private LocalTime workEnd;
}
