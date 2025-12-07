package com.example.demo.order.dto;

import com.example.demo.order.model.OrderStatus;
import com.example.demo.order.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String title;
    private String clientEmail;
    private String driverEmail;
    
    private String pickupLocation;
    private String pickupAddress;
    private LocalDate pickupDate;
    
    private String deliveryLocation;
    private String deliveryAddress;
    private LocalDate deliveryDeadline;
    
    private VehicleType vehicleType;
    private Double cargoWeight;
    private String description;

    private double price;
    
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
}
