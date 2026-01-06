package com.example.demo.dispatch.controller;

import com.example.demo.dispatch.dto.RouteResponse;
import com.example.demo.dispatch.service.DriverService;
import com.example.demo.order.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
public class DriverController {

    private final DriverService driverService;

    @Data
    @AllArgsConstructor
    static class ErrorResponse {
        private String message;
    }

    @Data
    @AllArgsConstructor
    static class SuccessResponse {
        private String message;
    }

    // ========== TRASY KIEROWCY ==========

    @GetMapping("/routes")
    public ResponseEntity<List<RouteResponse>> getMyRoutes(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(driverService.getRoutesForDriver(email));
    }

    @GetMapping("/routes/active")
    public ResponseEntity<List<RouteResponse>> getMyActiveRoutes(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(driverService.getActiveRoutesForDriver(email));
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<?> getRouteById(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            return ResponseEntity.ok(driverService.getRouteByIdForDriver(id, email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========== ZLECENIA KIEROWCY ==========

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(driverService.getOrdersForDriver(email));
    }

    @GetMapping("/orders/active")
    public ResponseEntity<List<OrderResponse>> getMyActiveOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(driverService.getActiveOrdersForDriver(email));
    }

    // ========== ZMIANA STATUSU TRASY ==========

    @PutMapping("/routes/{id}/start")
    public ResponseEntity<?> startRoute(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            RouteResponse response = driverService.startRoute(id, email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/routes/{id}/complete")
    public ResponseEntity<?> completeRoute(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            RouteResponse response = driverService.completeRoute(id, email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========== ZMIANA STATUSU ZLECENIA ==========

    @PutMapping("/orders/{id}/pickup")
    public ResponseEntity<?> pickupOrder(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            OrderResponse response = driverService.pickupOrder(id, email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/orders/{id}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            OrderResponse response = driverService.deliverOrder(id, email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========== GRAFIK PRACY ==========

    @GetMapping("/schedule")
    public ResponseEntity<?> getMySchedule(Authentication authentication) {
        try {
            String email = authentication.getName();
            return ResponseEntity.ok(driverService.getScheduleForDriver(email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========== STATYSTYKI KIEROWCY ==========

    @GetMapping("/stats")
    public ResponseEntity<?> getMyStats(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(driverService.getDriverStats(email));
    }
}
