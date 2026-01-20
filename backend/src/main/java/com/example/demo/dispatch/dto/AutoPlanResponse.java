package com.example.demo.dispatch.dto;

import com.example.demo.dispatch.model.AutoPlanningStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoPlanResponse {
    private Long planningId;

    private AutoPlanningStatus status;

    private LocalDate planningDate;

    private List<AutoPlanRoute> routes;
}
