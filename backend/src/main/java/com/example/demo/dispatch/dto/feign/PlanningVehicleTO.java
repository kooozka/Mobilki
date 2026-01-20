package com.example.demo.dispatch.dto.feign;

import com.example.demo.order.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningVehicleTO {

    private Long id;

    private VehicleType vehicleType;
}
