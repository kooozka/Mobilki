package com.example.demo.dispatch.service;

import com.example.demo.dispatch.dto.DriverScheduleResponse;
import com.example.demo.dispatch.dto.DriverStatsResponse;
import com.example.demo.dispatch.dto.RouteResponse;
import com.example.demo.dispatch.model.DriverSchedule;
import com.example.demo.dispatch.model.Route;
import com.example.demo.dispatch.model.RouteStatus;
import com.example.demo.dispatch.repository.DriverScheduleRepository;
import com.example.demo.dispatch.repository.RouteRepository;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.model.Order;
import com.example.demo.order.model.OrderStatus;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.security.model.User;
import com.example.demo.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final RouteRepository routeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DriverScheduleRepository driverScheduleRepository;

    // ========== POBIERANIE TRAS ==========

    public List<RouteResponse> getRoutesForDriver(String email) {
        User driver = getUserByEmail(email);
        return routeRepository.findByDriverOrderByRouteDateDesc(driver).stream()
                .map(this::mapRouteToResponse)
                .collect(Collectors.toList());
    }

    public List<RouteResponse> getActiveRoutesForDriver(String email) {
        User driver = getUserByEmail(email);
        return routeRepository.findByDriverAndStatusIn(driver,
                List.of(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS)).stream()
                .map(this::mapRouteToResponse)
                .collect(Collectors.toList());
    }

    public RouteResponse getRouteByIdForDriver(Long routeId, String email) {
        User driver = getUserByEmail(email);
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono trasy"));

        if (!route.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Nie masz dostępu do tej trasy");
        }

        return mapRouteToResponse(route);
    }

    // ========== POBIERANIE ZLECEŃ ==========

    public List<OrderResponse> getOrdersForDriver(String email) {
        User driver = getUserByEmail(email);
        return orderRepository.findByDriverOrderByPickupDateDesc(driver).stream()
                .map(this::mapOrderToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getActiveOrdersForDriver(String email) {
        User driver = getUserByEmail(email);
        return orderRepository.findByDriverAndStatusIn(driver,
                List.of(OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS)).stream()
                .map(this::mapOrderToResponse)
                .collect(Collectors.toList());
    }

    // ========== ZMIANA STATUSU TRASY ==========

    @Transactional
    public RouteResponse startRoute(Long routeId, String email) {
        User driver = getUserByEmail(email);
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono trasy"));

        if (!route.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Nie masz dostępu do tej trasy");
        }

        if (route.getStatus() != RouteStatus.PLANNED) {
            throw new RuntimeException("Można rozpocząć tylko zaplanowaną trasę");
        }

        route.setStatus(RouteStatus.IN_PROGRESS);

        // Zmień status wszystkich zleceń na IN_PROGRESS
        for (Order order : route.getOrders()) {
            if (order.getStatus() == OrderStatus.ASSIGNED) {
                order.setStatus(OrderStatus.IN_PROGRESS);
                orderRepository.save(order);
            }
        }

        return mapRouteToResponse(routeRepository.save(route));
    }

    @Transactional
    public RouteResponse completeRoute(Long routeId, String email) {
        User driver = getUserByEmail(email);
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono trasy"));

        if (!route.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Nie masz dostępu do tej trasy");
        }

        if (route.getStatus() != RouteStatus.IN_PROGRESS) {
            throw new RuntimeException("Można zakończyć tylko trasę w trakcie realizacji");
        }

        // Sprawdź czy wszystkie zlecenia są zakończone
        boolean allCompleted = route.getOrders().stream()
                .allMatch(order -> order.getStatus() == OrderStatus.COMPLETED);

        if (!allCompleted) {
            throw new RuntimeException("Nie wszystkie zlecenia zostały dostarczone");
        }

        route.setStatus(RouteStatus.COMPLETED);
        return mapRouteToResponse(routeRepository.save(route));
    }

    // ========== ZMIANA STATUSU ZLECENIA ==========

    @Transactional
    public OrderResponse pickupOrder(Long orderId, String email) {
        User driver = getUserByEmail(email);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zlecenia"));

        if (order.getDriver() == null || !order.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Nie masz dostępu do tego zlecenia");
        }

        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new RuntimeException("Można odebrać tylko przypisane zlecenie");
        }

        order.setStatus(OrderStatus.IN_PROGRESS);
        return mapOrderToResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse deliverOrder(Long orderId, String email) {
        User driver = getUserByEmail(email);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zlecenia"));

        if (order.getDriver() == null || !order.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Nie masz dostępu do tego zlecenia");
        }

        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new RuntimeException("Można dostarczyć tylko zlecenie w trakcie realizacji");
        }

        order.setStatus(OrderStatus.COMPLETED);
        return mapOrderToResponse(orderRepository.save(order));
    }

    // ========== GRAFIK PRACY ==========

    public DriverScheduleResponse getScheduleForDriver(String email) {
        User driver = getUserByEmail(email);
        List<DriverSchedule> schedules = driverScheduleRepository.findByDriver(driver);
        if (schedules.isEmpty()) {
            throw new RuntimeException("Nie masz przypisanego grafiku pracy");
        }
        return mapScheduleToResponse(schedules.get(0));
    }

    // ========== STATYSTYKI ==========

    public DriverStatsResponse getDriverStats(String email) {
        User driver = getUserByEmail(email);

        long totalOrders = orderRepository.countByDriver(driver);
        long completedOrders = orderRepository.countByDriverAndStatus(driver, OrderStatus.COMPLETED);
        long activeOrders = orderRepository.countByDriverAndStatusIn(driver,
                List.of(OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS));

        long totalRoutes = routeRepository.countByDriver(driver);
        long completedRoutes = routeRepository.countByDriverAndStatus(driver, RouteStatus.COMPLETED);
        long activeRoutes = routeRepository.countByDriverAndStatusIn(driver,
                List.of(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS));

        Double totalDistance = routeRepository.sumTotalDistanceByDriverAndStatus(driver, RouteStatus.COMPLETED);

        return DriverStatsResponse.builder()
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .activeOrders(activeOrders)
                .totalRoutes(totalRoutes)
                .completedRoutes(completedRoutes)
                .activeRoutes(activeRoutes)
                .totalDistanceKm(totalDistance != null ? totalDistance : 0.0)
                .build();
    }

    // ========== POMOCNICZE ==========

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));
    }

    private RouteResponse mapRouteToResponse(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .driverId(route.getDriver().getId())
                .driverEmail(route.getDriver().getEmail())
                .vehicleId(route.getVehicle().getId())
                .vehicleRegistration(route.getVehicle().getRegistrationNumber())
                .routeDate(route.getRouteDate())
                .orders(route.getOrders().stream()
                        .map(this::mapOrderToResponse)
                        .collect(Collectors.toList()))
                .totalDistance(route.getTotalDistance())
                .estimatedTimeMinutes(route.getEstimatedTimeMinutes())
                .status(route.getStatus())
                .createdAt(route.getCreatedAt())
                .build();
    }

    private OrderResponse mapOrderToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .title(order.getTitle())
                .clientEmail(order.getClient().getEmail())
                .driverEmail(order.getDriver() != null ? order.getDriver().getEmail() : null)
                .pickupLocation(order.getPickupLocation())
                .pickupAddress(order.getPickupAddress())
                .pickupDate(order.getPickupDate())
                .deliveryLocation(order.getDeliveryLocation())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryDeadline(order.getDeliveryDeadline())
                .vehicleType(order.getVehicleType())
                .cargoWeight(order.getCargoWeight())
                .description(order.getDescription())
                .price(order.getPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .confirmedAt(order.getConfirmedAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .build();
    }

    private DriverScheduleResponse mapScheduleToResponse(DriverSchedule schedule) {
        return DriverScheduleResponse.builder()
                .id(schedule.getId())
                .driverId(schedule.getDriver().getId())
                .driverEmail(schedule.getDriver().getEmail())
                .workDays(schedule.getWorkDays())
                .workStartTime(schedule.getWorkStartTime())
                .workEndTime(schedule.getWorkEndTime())
                .active(schedule.getActive())
                .build();
    }
}
