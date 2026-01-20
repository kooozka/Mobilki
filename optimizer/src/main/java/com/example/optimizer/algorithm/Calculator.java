package com.example.optimizer.algorithm;

import com.example.optimizer.model.*;

import java.util.*;

public class Calculator {

    private static final List<VehicleType> VEHICLE_TYPES = Arrays.asList(VehicleType.values());
    private static final int SERVICE_TIME_MINUTES = 15;

    public static double calculateFitness(Solution solution, RunningOptimization optimization) {

        var pair = calculateDurations(solution, optimization);
        List<Double> routeDistances = pair.first;
        List<Double> routeDurations = pair.second;

        var driversAndVehicles = assignDriversAndVehicles(solution, routeDurations, optimization);
        if (driversAndVehicles.size() < routeDistances.size()) {
            return Double.MAX_VALUE;
        }
        double fitness = 0.0;
        for (int i = 0; i < routeDistances.size(); i++) {
            fitness += routeDistances.get(i);
        }
        return fitness;
    }

    private static Pair<List<Double>, List<Double>> calculateDurations(Solution solution, RunningOptimization optimization) {
        int currentLocation = 0;
        double distance = 0.0;
        double duration = 0.0;
        List<Double> routeDistances = new ArrayList<>();
        List<Double> routeDurations = new ArrayList<>();
        var sequence = solution.getSequence();
        var distanceMatrix = optimization.getDistanceMatrix();
        var durationMatrix = optimization.getDurationMatrix();
        for (int i = 0; i < sequence.size(); i++) {
            int gene = sequence.get(i);
            if (gene < 0) {
                distance += distanceMatrix[currentLocation][0];
                duration += durationMatrix[currentLocation][0];
                routeDistances.add(distance);
                routeDurations.add(duration);
                distance = 0.0;
                duration = 0.0;
                currentLocation = 0;
            } else {
                distance += distanceMatrix[currentLocation][gene + 1] + distanceMatrix[gene+1][gene+1];
                duration += durationMatrix[currentLocation][gene + 1] + durationMatrix[gene+1][gene+1] + SERVICE_TIME_MINUTES * 2;
                currentLocation = gene + 1;
            }
        }
        distance += distanceMatrix[currentLocation][0];
        duration += durationMatrix[currentLocation][0];
        routeDistances.add(distance);
        routeDurations.add(duration);
        return new Pair<>(routeDistances, routeDurations);
    }

    private static List<Pair<Long, Long>> assignDriversAndVehicles(Solution solution, List<Double> routeDurations, RunningOptimization optimization) {
        List<Pair<Long, Long>> assignments = new ArrayList<>();
        List<Driver> drivers = new ArrayList<>(optimization.getDrivers());
        drivers.sort(Comparator.comparingInt(d -> (d.getWorkEnd().toSecondOfDay() - d.getWorkStart().toSecondOfDay())));
        List<Integer> minVehicleTypeIndexes = getMinVehicleTypeIndexes(solution, optimization.getOrders());

        HashMap<VehicleType, List<Long>> vehicleTypeToVehicleIds = new HashMap<>();
        for (var vehicle : optimization.getVehicles()) {
            vehicleTypeToVehicleIds
                    .computeIfAbsent(vehicle.getVehicleType(), k -> new ArrayList<>())
                    .add(vehicle.getId());
        }

        int routeIndex = 0;
        for (var duration : routeDurations) {
            if (duration <= 0.0) {
                routeIndex++;
                assignments.add(new Pair<>(null, null));
                continue;
            }
            int minVehicleTypeIndex = minVehicleTypeIndexes.get(routeIndex);
            boolean driverFound = false;
            for (int vtIndex = minVehicleTypeIndex; vtIndex < VEHICLE_TYPES.size(); vtIndex++) {
                if (!vehicleTypeToVehicleIds.containsKey(VEHICLE_TYPES.get(vtIndex)) ||
                        vehicleTypeToVehicleIds.get(VEHICLE_TYPES.get(vtIndex)).isEmpty()) {
                    continue;
                }
                for (Driver driver : drivers) {
                    VehicleType vehicleType = VEHICLE_TYPES.get(vtIndex);
                    if (driver.getLicences().contains(vehicleType) &&
                            (driver.getWorkEnd().toSecondOfDay() - driver.getWorkStart().toSecondOfDay()) >= duration * 60) {
                        // kierowca może obsłużyć trasę
                        assignments.add(new Pair<>(driver.getId(), vehicleTypeToVehicleIds.get(vehicleType).remove(0)));
                        drivers.remove(driver);
                        driverFound = true;
                        break;
                    }
                }
                if (driverFound) {
                    break;
                }
            }
            routeIndex++;
        }
        return assignments;
    }

    public record Pair<T, U>(T first, U second) {}

    public static List<OptimizedRoute> solutionToOptimizedRoutes(Solution solution, RunningOptimization optimization) {
        var pair = calculateDurations(solution, optimization);
        List<Double> routeDistances = pair.first;
        List<Double> routeDurations = pair.second;

        var driversAndVehicles = assignDriversAndVehicles(solution, routeDurations, optimization);
        if (driversAndVehicles.size() < routeDistances.size()) {
            return null;
        }

        List<List<Long>> routeOrderIds = new ArrayList<>();
        List<Long> array = new ArrayList<>();
        for (int i = 0; i < solution.getSequence().size(); i++)
        {
            int gene = solution.getSequence().get(i);
            if (gene < 0) {
                routeOrderIds.add(array);
                array = new ArrayList<>();
            } else {
                array.add(optimization.getOrders().get(gene).getId());
            }
        }
        routeOrderIds.add(array);

        List<OptimizedRoute> optimizedRoutes = new ArrayList<>();
        for (int i = 0; i < routeDistances.size(); i++) {
            if (routeDistances.get(i) <= 0.0) {
                continue;
            }
            OptimizedRoute optimizedRoute = OptimizedRoute.builder()
                            .driverId(driversAndVehicles.get(i).first)
                            .vehicleId(driversAndVehicles.get(i).second)
                            .orderIdsOrdered(routeOrderIds.get(i))
                            .estimatedTimeMinutes(routeDurations.get(i).intValue())
                            .totalDistance(routeDistances.get(i))
                            .build();

            optimizedRoutes.add(optimizedRoute);
        }
        return optimizedRoutes;
    }


    private static List<Integer> getMinVehicleTypeIndexes(Solution solution, List<Order> orders) {
        int minIndex = 0;
        List<Integer> minIndexes = new ArrayList<>();
        for (int i = 0; i < solution.getSequence().size(); i++) {
            if (solution.getSequence().get(i) < 0) {
                minIndexes.add(minIndex);
                minIndex = 0;
                continue;
            }
            Order order = orders.get(solution.getSequence().get(i));
            while (order.getCargoWeight() > VEHICLE_TYPES.get(minIndex).getMaxWeight()) {
                minIndex++;
            }
        }
        minIndexes.add(minIndex);
        return minIndexes;
    }
}
