package com.example.demo.dispatch.dto;

import com.example.demo.order.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRequest {
    private String registrationNumber;
    private String brand;
    private String model;
    private VehicleType type;
    private Double maxWeight;
    private Boolean available;
    private String notes;
}
