package com.example.optimizer.dto;

import com.example.optimizer.model.OptimizedRoute;
import com.example.optimizer.model.OptimizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizerResponse {

    private OptimizationStatus status;

    private List<OptimizedRoute> routes = null;
}
