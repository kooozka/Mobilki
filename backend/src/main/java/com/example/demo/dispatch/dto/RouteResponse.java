package com.example.demo.dispatch.dto;

import com.example.demo.dispatch.model.RouteStatus;
import com.example.demo.order.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private Long id;
    private Long driverId;
    private String driverEmail;
    private Long vehicleId;
    private String vehicleRegistration;
    private LocalDate routeDate;
    private List<OrderResponse> orders;
    private Double totalDistance;
    private Integer estimatedTimeMinutes;
    private RouteStatus status;
    private LocalDateTime createdAt;
}
