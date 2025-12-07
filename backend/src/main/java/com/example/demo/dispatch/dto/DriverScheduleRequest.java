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
public class DriverScheduleRequest {
    private Long driverId;
    private Long vehicleId;
    private Set<DayOfWeek> workDays;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Boolean active;
}
