package com.example.demo.dispatch.controller;

import com.example.demo.dispatch.dto.*;
import com.example.demo.dispatch.service.DriverScheduleService;
import com.example.demo.dispatch.service.RoutePlanningService;
import com.example.demo.dispatch.service.VehicleService;
import com.example.demo.order.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/routes/plan")
    public ResponseEntity<?> planRoutes(@RequestBody PlanRouteRequest request) {
        try {
            List<RouteResponse> routes = routePlanningService.planRoutes(request.getOrderIds());
            return ResponseEntity.ok(routes);
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
}
