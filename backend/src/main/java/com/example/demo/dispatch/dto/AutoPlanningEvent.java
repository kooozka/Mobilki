package com.example.demo.dispatch.dto;

import com.example.demo.dispatch.model.AutoPlanningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoPlanningEvent {
    private Long planningId;
    private AutoPlanningStatus status;
    private LocalDate planningDate;
}
