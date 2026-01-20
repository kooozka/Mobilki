package com.example.optimizer.algorithm;

import com.example.optimizer.model.OptimizationStatus;
import com.example.optimizer.model.OptimizedRoute;
import com.example.optimizer.model.RunningOptimization;
import com.example.optimizer.repository.RunningOptimizationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class TabuSearchOptimizer {


    @Async
    public void optimize(RunningOptimization optimization) {
        // Parameters (tune as needed)
        final int MAX_ITERATIONS = 1000;
        final int TABU_TENURE = 50;
        final int NEIGHBOR_SAMPLES = 20;
        final int NO_IMPROVEMENT_LIMIT = 50;

        Instant start = Instant.now();

        int orderCount = optimization.getOrders().size();
        int groupCount = Math.min(optimization.getVehicles().size(), optimization.getDrivers().size());

        Solution current = new Solution(orderCount, groupCount);
        Solution best = current.copy();
        double bestFitness = best.fitness(optimization);

        Deque<String> tabuQueue = new ArrayDeque<>();
        Set<String> tabuSet = new HashSet<>();

        int iterationsWithoutImprovement = 0;

        Random rnd = new Random();

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Solution bestCandidate = null;
            double bestCandidateFitness = Double.MAX_VALUE;
            String bestCandidateMoveKey = null;

            int n = current.getSequence().size();
            if (n < 2) break;

            // sample neighborhood (random swaps)
            for (int s = 0; s < NEIGHBOR_SAMPLES; s++) {
                int i = rnd.nextInt(n);
                int j = rnd.nextInt(n);
                if (i == j) continue;

                // canonicalize move key as "min:max"
                int a = Math.min(i, j);
                int b = Math.max(i, j);
                String moveKey = a + ":" + b;

                // build candidate
                Solution candidate = current.copy();
                Collections.swap(candidate.getSequence(), a, b);

                double candidateFitness = candidate.fitness(optimization);

                boolean isTabu = tabuSet.contains(moveKey);
                boolean aspiration = candidateFitness < bestFitness;

                if (isTabu && !aspiration) {
                    continue;
                }

                if (candidateFitness < bestCandidateFitness) {
                    bestCandidate = candidate;
                    bestCandidateFitness = candidateFitness;
                    bestCandidateMoveKey = moveKey;
                }
            }

            if (bestCandidate == null) {
                // no valid candidate found in this iteration
                break;
            }

            // move to the best candidate
            current = bestCandidate;

            // update tabu list (on the move we performed)
            if (bestCandidateMoveKey != null) {
                tabuQueue.addLast(bestCandidateMoveKey);
                tabuSet.add(bestCandidateMoveKey);
                if (tabuQueue.size() > TABU_TENURE) {
                    String removed = tabuQueue.removeFirst();
                    tabuSet.remove(removed);
                }
            }

            // update global best
            if (bestCandidateFitness < bestFitness) {
                best = current.copy();
                bestFitness = bestCandidateFitness;
                iterationsWithoutImprovement = 0;
            } else {
                iterationsWithoutImprovement++;
            }

            if (iterationsWithoutImprovement >= NO_IMPROVEMENT_LIMIT) {
                break;
            }
        }

        Instant end = Instant.now();
        log.info("Optimization finished in {} ms", Duration.between(start, end).toMillis());

        // convert best solution to optimized routes and log basic info
        var optimizedRoutes = Calculator.solutionToOptimizedRoutes(best, optimization);
        if (optimizedRoutes == null) {
            log.info("TabuSearch: no feasible assignment found.");
            optimization.setStatus(OptimizationStatus.FAILED);
        } else {
            log.info("TabuSearch: found " + optimizedRoutes.size() + " routes, total distance approx = " +
                    optimizedRoutes.stream().mapToDouble(OptimizedRoute::getTotalDistance).sum());
            optimization.setRoutes(optimizedRoutes);
            optimization.setStatus(OptimizationStatus.COMPLETED);
        }
    }
}
