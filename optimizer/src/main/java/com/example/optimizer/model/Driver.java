package com.example.optimizer.model;

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
public class Driver {
    private Long id;

    private Set<VehicleType> licences;

    private LocalTime workStart;

    private LocalTime workEnd;
}
