package com.example.demo.dispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {
    private Long id;
    private String email;
    private Boolean hasActiveSchedule;
    // assignedVehicle usunięte - pojazd przypisywany do trasy
}
