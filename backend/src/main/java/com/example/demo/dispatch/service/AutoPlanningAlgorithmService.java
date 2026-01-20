package com.example.demo.dispatch.service;

import com.example.demo.dispatch.dto.AutoPlanResponse;
import com.example.demo.dispatch.dto.AutoPlanRoute;
import com.example.demo.dispatch.dto.AutoPlanningEvent;
import com.example.demo.dispatch.dto.RouteResponse;
import com.example.demo.dispatch.dto.feign.AutoPlanOptimizerRequest;
import com.example.demo.dispatch.dto.feign.PlanningDriverTO;
import com.example.demo.dispatch.dto.feign.PlanningOrderTO;
import com.example.demo.dispatch.dto.feign.PlanningVehicleTO;
import com.example.demo.dispatch.feign.RouteOptimizerClient;
import com.example.demo.dispatch.model.*;
import com.example.demo.dispatch.model.json.AutoPlanningResult;
import com.example.demo.dispatch.repository.AutoPlanningRepository;
import com.example.demo.dispatch.repository.DriverRepository;
import com.example.demo.dispatch.repository.VehicleRepository;
import com.example.demo.order.model.Order;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.order.service.OrderService;
import com.example.demo.security.model.User;
import com.example.demo.security.repository.UserRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPlanningAlgorithmService {

    private final RouteOptimizerClient optimizerClient;

    private final AutoPlanningRepository autoPlanningRepository;
    private final OrderRepository orderRepository;

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    private final AutoPlanningService autoPlanningService;

    private final GoogleMapsService googleMapsService;

    private final RoutePlanningService routePlanningService;
    private final OrderService orderService;

    @Value("${vehicle.base.address}")
    private String vehicleBaseAddress;

    public AutoPlanResponse getOptimizationResult(String userEmail, LocalDate planningDate) {
        User user = userRepository.findByEmailAndSuspendedIsFalse(userEmail).orElseThrow();
        var autoPlanning = autoPlanningRepository.findFirstByAuthorAndPlanningDateOrderByStartedAtDesc(user, planningDate);
        if (autoPlanning == null)
            return null;
        return toAutoPlanResponse(autoPlanning);
    }

    private AutoPlanResponse toAutoPlanResponse(AutoPlanning autoPlanning) {
        return AutoPlanResponse.builder()
                .planningId(autoPlanning.getId())
                .status(autoPlanning.getStatus())
                .planningDate(autoPlanning.getPlanningDate())
                .routes(
                        autoPlanning.getResult() != null ?
                        autoPlanning.getResult().getRoutes().stream().map(route -> {
                            User driver = route.getDriverId() != null ? userRepository.findById(route.getDriverId()).orElse(null) : null;
                            Vehicle vehicle = route.getVehicleId() != null ? vehicleRepository.findById(route.getVehicleId()).orElse(null) : null;
                            return AutoPlanRoute.builder()
                                    .driverId(route.getDriverId())
                                    .driverEmail(driver != null ? driver.getEmail() : null)
                                    .vehicleId(route.getVehicleId())
                                    .vehicleRegistration(vehicle != null ? vehicle.getRegistrationNumber() : null)
                                    .routeDate(autoPlanning.getPlanningDate())
                                    .totalDistance(route.getTotalDistance())
                                    .estimatedTimeMinutes(route.getEstimatedTimeMinutes())
                                    .orders(route.getOrderIdsOrdered().stream().map(orderId -> {
                                        Order order = orderRepository.findById(orderId).orElse(null);
                                        if (order != null) {
                                            return orderService.mapToResponse(order);
                                        } else {
                                            return null;
                                        }
                                    }).filter(Objects::nonNull).toList())
                                    .build();
                        }).toList()
                                : null
                ).build();
    }

    @Transactional
    public void autoPlanRoutes(String email, LocalDate routeDate, List<Long> orderIds) {
        User user = userRepository.findByEmailAndSuspendedIsFalse(email).orElseThrow();
        if (autoPlanningRepository.countAllByAuthorAndStatusInAndPlanningDate(user, List.of(AutoPlanningStatus.IN_PROGRESS, AutoPlanningStatus.COMPLETED), routeDate) > 0) {
            throw new IllegalStateException("Istnieje już trwające lub oczekujące automatyczne planowanie dla tej daty.");
        }

        List<Order> orders = orderRepository.findAllById(orderIds);
        autoPlanningService.validateOrders(orders, orderIds, routeDate);

        List<DriverSchedule> availableSchedules = autoPlanningService.getAvailableSchedules(routeDate);
        List<PlanningDriverTO> drivers = availableSchedules.stream()
                .map(schedule -> {
                    Driver driver = schedule.getDriver();
                    return PlanningDriverTO.builder()
                            .id(driver.getId())
                            .licences(driver.getLicenseTypes())
                            .workStart(schedule.getWorkStartTime())
                            .workEnd(schedule.getWorkEndTime())
                            .build();
                })
                .toList();

        List<Vehicle> availableVehicles = autoPlanningService.getAvailableVehicles(routeDate);
        List<PlanningVehicleTO> vehicles = availableVehicles.stream()
                .map(vehicle -> PlanningVehicleTO.builder()
                        .id(vehicle.getId())
                        .vehicleType(vehicle.getType())
                        .build()
                )
                .toList();

        List<PlanningOrderTO> plannedOrders = orders.stream()
                .map(order -> PlanningOrderTO.builder()
                        .id(order.getId())
                        .cargoWeight(order.getCargoWeight())
                        .build()
                ).toList();

        AutoPlanning autoPlanning = AutoPlanning.builder()
                .author(user)
                .planningDate(routeDate)
                .startedAt(LocalDateTime.now())
                .consumed(false)
                .status(AutoPlanningStatus.IN_PROGRESS)
                .build();

        autoPlanning = autoPlanningRepository.save(autoPlanning);

        List<String> origins = new ArrayList<>(List.of(vehicleBaseAddress));
        origins.addAll(orders.stream().map(o -> o.getDeliveryAddress() + ' ' + o.getDeliveryLocation()).toList());
        List<String> destinations = new ArrayList<>(List.of(vehicleBaseAddress));
        destinations.addAll(orders.stream().map(o -> o.getPickupAddress() + ' ' + o.getPickupLocation()).toList());
        var distanceMatrix = googleMapsService.getDistanceAndDurationMatrix(origins, destinations);

        try {
            optimizerClient.optimize(AutoPlanOptimizerRequest.builder()
                    .planningId(autoPlanning.getId())
                    .drivers(Set.copyOf(drivers))
                    .vehicles(Set.copyOf(vehicles))
                    .orders(plannedOrders)
                    .distanceMatrix(distanceMatrix.distanceMatrix)
                    .durationMatrix(distanceMatrix.durationMatrix)
                    .build()
            );
        } catch (Exception e) {
            log.error("Błąd podczas inicjowania automatycznego planowania dla zadania: " + autoPlanning.getId(), e);
            autoPlanning.setStatus(AutoPlanningStatus.FAILED);
            autoPlanningRepository.save(autoPlanning);
            throw new RuntimeException("Nie udało się zainicjować automatycznego planowania tras.");
        }
    }

    public void checkAndProcessAutoPlanningResults() {
        var autoPlannings = autoPlanningRepository.findAllByStatus(AutoPlanningStatus.IN_PROGRESS);
        for (AutoPlanning autoPlanning : autoPlannings) {
            try {
                var response = optimizerClient.getOptimizationResult(autoPlanning.getId());
                if (response != null && response.getStatus() != null) {
                    autoPlanning.setStatus(response.getStatus());
                    if (response.getStatus() == AutoPlanningStatus.COMPLETED) {
                        autoPlanning.setResult(AutoPlanningResult.builder().routes(response.getRoutes()).build());
                    } else if (response.getStatus() == AutoPlanningStatus.FAILED) {
                        log.warn("Automatyczne planowanie zakończyło się niepowodzeniem dla zadania: " + autoPlanning.getId());
                    }
                    autoPlanningRepository.save(autoPlanning);
                }
            } catch (FeignException.NotFound e) {
                log.info("Nie rozpoznano zadania automatycznego planowania o ID: " + autoPlanning.getId());
            } catch (Exception e) {
                log.error("Błąd podczas pobierania wyników automatycznego planowania dla zadania: " + autoPlanning.getId(), e);
            }
        }
    }

    public List<AutoPlanResponse> getPendingAutoPlannings(String email) {
        var autoPlannings = autoPlanningRepository.findAllByAuthor_EmailAndStatus(email, AutoPlanningStatus.IN_PROGRESS);
        return autoPlannings.stream().map(this::toAutoPlanResponse).toList();
    }

    public AutoPlanResponse getAwaitingAutoPlanning(String email) {
        var autoPlanning = autoPlanningRepository.findFirstByAuthor_EmailAndStatusOrderByPlanningDateAsc(email, AutoPlanningStatus.COMPLETED);
        if (autoPlanning == null)
            return null;

        return toAutoPlanResponse(autoPlanning);
    }

    @Transactional
    public void rejectAutoPlanning(Long planningId, String email) {
        User user = userRepository.findByEmailAndSuspendedIsFalse(email).orElseThrow();
        AutoPlanning autoPlanning = autoPlanningRepository.findById(planningId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono automatycznego planowania o ID: " + planningId));
        if (!autoPlanning.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Nie masz uprawnień do odrzucenia tego automatycznego planowania.");
        }
        if (autoPlanning.getStatus() != AutoPlanningStatus.COMPLETED) {
            throw new RuntimeException("Można odrzucić tylko zakończone automatyczne planowanie.");
        }
        autoPlanning.setConsumed(true);
        autoPlanning.setStatus(AutoPlanningStatus.REJECTED);
        autoPlanningRepository.save(autoPlanning);
    }

    @Transactional
    public List<RouteResponse> acceptAutoPlanning(Long planningId, String email) {
        User user = userRepository.findByEmailAndSuspendedIsFalse(email).orElseThrow();
        AutoPlanning autoPlanning = autoPlanningRepository.findById(planningId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono automatycznego planowania o ID: " + planningId));
        if (!autoPlanning.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Nie masz uprawnień do zatwierdzenia tego automatycznego planowania.");
        }
        if (autoPlanning.getStatus() != AutoPlanningStatus.COMPLETED) {
            throw new RuntimeException("Można zatwierdzić tylko zakończone automatyczne planowanie.");
        }
        autoPlanning.setConsumed(true);
        autoPlanning.setStatus(AutoPlanningStatus.ACCEPTED);
        autoPlanningRepository.save(autoPlanning);
        return routePlanningService.createRouteFromAutoPlanning(autoPlanning);
    }

    public List<AutoPlanningEvent> getAutoPlanningEvents(String email) {
        var autoPlannings = autoPlanningRepository.findAllByConsumedIsFalseAndAuthor_EmailAndStatusIsIn(email, List.of(AutoPlanningStatus.COMPLETED, AutoPlanningStatus.FAILED));
        return autoPlannings.stream().map(ap ->
                AutoPlanningEvent.builder()
                        .planningId(ap.getId())
                        .planningDate(ap.getPlanningDate())
                        .status(ap.getStatus())
                        .build()
        ).toList();
    }

    @Transactional
    public void consumeAutoPlanning(Long planningId, String email) {
        User user = userRepository.findByEmailAndSuspendedIsFalse(email).orElseThrow();
        AutoPlanning autoPlanning = autoPlanningRepository.findById(planningId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono automatycznego planowania o ID: " + planningId));
        if (!autoPlanning.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Nie masz uprawnień do zatwierdzenia tego automatycznego planowania.");
        }
        if (autoPlanning.getStatus() == AutoPlanningStatus.IN_PROGRESS) {
            throw new RuntimeException("Można zatwierdzić tylko zakończone automatyczne planowanie.");
        }
        autoPlanning.setConsumed(true);
        autoPlanningRepository.save(autoPlanning);
    }
}
