package com.example.demo.dispatch.dto.feign;

import com.example.demo.dispatch.dto.AutoPlanResponse;
import com.example.demo.dispatch.model.AutoPlanningStatus;
import com.example.demo.dispatch.model.json.AutoPlanningRoute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoPlanOptimizerResponse {

    private AutoPlanningStatus status;

    private List<AutoPlanningRoute> routes;
}
