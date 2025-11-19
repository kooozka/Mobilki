package com.example.demo.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;
    
    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;
    
    @NotNull(message = "Pickup time from is required")
    private LocalDateTime pickupTimeFrom;
    
    @NotNull(message = "Pickup time to is required")
    private LocalDateTime pickupTimeTo;
    
    @NotBlank(message = "Delivery location is required")
    private String deliveryLocation;
    
    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;
    
    @NotNull(message = "Delivery time from is required")
    private LocalDateTime deliveryTimeFrom;
    
    @NotNull(message = "Delivery time to is required")
    private LocalDateTime deliveryTimeTo;
    
    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;
    
    @NotNull(message = "Cargo weight is required")
    @Positive(message = "Cargo weight must be positive")
    private Double cargoWeight;
    
    private String description;
}
