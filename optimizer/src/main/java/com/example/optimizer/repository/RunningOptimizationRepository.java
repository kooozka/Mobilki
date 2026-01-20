package com.example.optimizer.repository;

import com.example.optimizer.model.RunningOptimization;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RunningOptimizationRepository {
    private final ConcurrentHashMap<Long, RunningOptimization> runningOptimizations = new ConcurrentHashMap<>();

    public void save(RunningOptimization optimization) {
        runningOptimizations.put(optimization.getId(), optimization);
    }

    public RunningOptimization findById(Long id) {
        return runningOptimizations.get(id);
    }
}
