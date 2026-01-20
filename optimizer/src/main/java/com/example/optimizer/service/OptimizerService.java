package com.example.optimizer.service;

import com.example.optimizer.algorithm.TabuSearchOptimizer;
import com.example.optimizer.dto.OptimizerRequest;
import com.example.optimizer.dto.OptimizerResponse;
import com.example.optimizer.model.OptimizationStatus;
import com.example.optimizer.model.RunningOptimization;
import com.example.optimizer.repository.RunningOptimizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OptimizerService {
    private final TabuSearchOptimizer optimizer;
    private final RunningOptimizationRepository repository;

    public void optimize(OptimizerRequest request) {
        var optimization = RunningOptimization.builder()
                .id(request.getPlanningId())
                .status(OptimizationStatus.IN_PROGRESS)
                .drivers(request.getDrivers())
                .orders(request.getOrders())
                .vehicles(request.getVehicles())
                .distanceMatrix(request.getDistanceMatrix())
                .durationMatrix(request.getDurationMatrix())
                .build();
        repository.save(optimization);
        optimizer.optimize(optimization);
    }

    public OptimizerResponse getOptimizationResult(Long id) {
        var optimization = repository.findById(id);
        if (optimization == null) {
            return null;
        }
        return OptimizerResponse.builder()
                .status(optimization.getStatus())
                .routes(optimization.getRoutes())
                .build();
    }
}
