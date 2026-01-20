package com.example.demo.dispatch.controller;

import com.example.demo.dispatch.dto.*;
import com.example.demo.dispatch.service.*;
import com.example.demo.order.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DISPATCH_MANAGER')")
public class DispatchController {

    private final RoutePlanningService routePlanningService;
    private final DriverScheduleService driverScheduleService;
    private final VehicleService vehicleService;
    private final AutoPlanningAlgorithmService autoPlanningService;

    @Data
    @AllArgsConstructor
    static class ErrorResponse {
        private String message;
    }

    // ========== ZLECENIA ==========

    @GetMapping("/orders/pending")
    public ResponseEntity<List<OrderResponse>> getPendingOrders() {
        return ResponseEntity.ok(routePlanningService.getPendingOrders());
    }

    @GetMapping("/orders/confirmed")
    public ResponseEntity<List<OrderResponse>> getConfirmedOrders() {
        return ResponseEntity.ok(routePlanningService.getConfirmedOrders());
    }

    @GetMapping("/orders/unassigned")
    public ResponseEntity<List<OrderResponse>> getUnassignedOrders() {
        return ResponseEntity.ok(routePlanningService.getAllUnassignedOrders());
    }

    @PutMapping("/orders/{id}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            OrderResponse response = routePlanningService.confirmOrder(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/orders/{id}/assign")
    public ResponseEntity<?> assignOrder(
            @PathVariable Long id,
            @RequestBody AssignOrderRequest request) {
        try {
            OrderResponse response = routePlanningService.assignOrderToDriver(id, request.getDriverId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========== KIEROWCY ==========

    @GetMapping("/drivers")
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverScheduleService.getAllDrivers());
    }

    // ========== GRAFIKI PRACY ==========

    @GetMapping("/schedules")
    public ResponseEntity<List<DriverScheduleResponse>> getAllSchedules() {
        return ResponseEntity.ok(driverScheduleService.getAllSchedules());
    }

    @GetMapping("/schedules/active")
    public ResponseEntity<List<DriverScheduleResponse>> getActiveSchedules() {
        return ResponseEntity.ok(driverScheduleService.getActiveSchedules());
    }

    @GetMapping("/schedules/{id}")
    public ResponseEntity<?> getScheduleById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(driverScheduleService.getScheduleById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/schedules/driver/{driverId}")
    public ResponseEntity<?> getScheduleByDriver(@PathVariable Long driverId) {
        try {
            return ResponseEntity.ok(driverScheduleService.getScheduleByDriver(driverId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/schedules")
    public ResponseEntity<?> createSchedule(@RequestBody DriverScheduleRequest request) {
        try {
            DriverScheduleResponse response = driverScheduleService.createSchedule(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<?> updateSchedule(
            @PathVariable Long id,
            @RequestBody DriverScheduleRequest request) {
        try {
            DriverScheduleResponse response = driverScheduleService.updateSchedule(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        try {
            driverScheduleService.deleteSchedule(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========== POJAZDY ==========

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping("/vehicles/available")
    public ResponseEntity<List<VehicleResponse>> getAvailableVehicles() {
        return ResponseEntity.ok(vehicleService.getAvailableVehicles());
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(vehicleService.getVehicleById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/vehicles")
    public ResponseEntity<?> createVehicle(@RequestBody VehicleRequest request) {
        try {
            VehicleResponse response = vehicleService.createVehicle(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<?> updateVehicle(
            @PathVariable Long id,
            @RequestBody VehicleRequest request) {
        try {
            VehicleResponse response = vehicleService.updateVehicle(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        try {
            vehicleService.deleteVehicle(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========== PLANOWANIE TRAS ==========

    @GetMapping("/routes/available-drivers/{date}")
    public ResponseEntity<?> getAvailableDriversForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            var drivers = routePlanningService.getAvailableDriversForDate(date).stream()
                    .map(driver -> new DriverResponse(driver.getId(), driver.getUser().getEmail(), true))
                    .toList();
            return ResponseEntity.ok(drivers);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/routes/available-vehicles/{date}")
    public ResponseEntity<?> getAvailableVehiclesForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            var vehicles = routePlanningService.getAvailableVehiclesForDate(date).stream()
                    .map(v -> vehicleService.mapToResponse(v))
                    .toList();
            return ResponseEntity.ok(vehicles);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/routes/plan")
    public ResponseEntity<?> planRoute(@RequestBody PlanRouteRequest request) {
        try {
            RouteResponse route = routePlanningService.planRoute(request);
            return ResponseEntity.ok(route);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/routes")
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        return ResponseEntity.ok(routePlanningService.getAllRoutes());
    }

    @GetMapping("/routes/date/{date}")
    public ResponseEntity<List<RouteResponse>> getRoutesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(routePlanningService.getRoutesByDate(date));
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<?> getRouteById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(routePlanningService.getRouteById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/routes/{id}/cancel")
    public ResponseEntity<?> cancelRoute(@PathVariable Long id) {
        try {
            RouteResponse response = routePlanningService.cancelRoute(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========== AUTO-PLANOWANIE ==========

    @PostMapping("/routes/auto-plan")
    public ResponseEntity<?> autoPlanRoutes(
            @RequestBody AutoPlanRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            autoPlanningService.autoPlanRoutes(
                    email,
                    request.getRouteDate(),
                    request.getOrderIds()
                    );
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/routes/auto-plan/pending")
    public ResponseEntity getPendingRoutes(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<AutoPlanResponse> responses = autoPlanningService.getPendingAutoPlannings(email);
            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/routes/auto-plan/awaiting")
    public ResponseEntity getAwaitingRoutes(Authentication authentication) {
        try {
            String email = authentication.getName();
            AutoPlanResponse response = autoPlanningService.getAwaitingAutoPlanning(email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("routes/auto-plan/events")
    public ResponseEntity getAutoPlanningEvents(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<AutoPlanningEvent> responses = autoPlanningService.getAutoPlanningEvents(email);
            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/routes/auto-plan/{id}/accept")
    public ResponseEntity<?> acceptAutoPlannedRoutes(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            autoPlanningService.acceptAutoPlanning(id, email);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/routes/auto-plan/{id}/reject")
    public ResponseEntity<?> rejectAutoPlannedRoutes(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            autoPlanningService.rejectAutoPlanning(id, email);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/routes/auto-plan/{id}/consume")
    public ResponseEntity<?> consumeAutoPlannedRoutes(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            autoPlanningService.consumeAutoPlanning(id, email);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/routes/auto-plan/{date}")
    public ResponseEntity<?> getAutoPlannedRoutes(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            AutoPlanResponse response = autoPlanningService.getOptimizationResult(email, date);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @Data
    static class AutoPlanRequest {
        private List<Long> orderIds;
        private LocalDate routeDate;
    }
}
