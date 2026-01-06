package com.example.demo.dispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverScheduleResponse {
    private Long id;
    private Long driverId;
    private String driverEmail;
    // vehicleId/vehicleRegistration usunięte - pojazd przypisywany do trasy
    private Set<DayOfWeek> workDays;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Boolean active;
}
