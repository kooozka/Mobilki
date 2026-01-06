package com.example.demo.dispatch.service;

import com.example.demo.dispatch.dto.PlanRouteRequest;
import com.example.demo.dispatch.dto.RouteResponse;
import com.example.demo.dispatch.model.DriverSchedule;
import com.example.demo.dispatch.model.Route;
import com.example.demo.dispatch.model.RouteStatus;
import com.example.demo.dispatch.model.Vehicle;
import com.example.demo.dispatch.repository.DriverScheduleRepository;
import com.example.demo.dispatch.repository.RouteRepository;
import com.example.demo.dispatch.repository.VehicleRepository;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.model.Order;
import com.example.demo.order.model.OrderStatus;
import com.example.demo.order.model.VehicleType;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.security.model.User;
import com.example.demo.security.model.UserRole;
import com.example.demo.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutePlanningService {

    private final OrderRepository orderRepository;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverScheduleRepository scheduleRepository;

    // Średnia prędkość pojazdu w km/h
    private static final double AVERAGE_SPEED_KMH = 50.0;
    // Czas obsługi jednego punktu w minutach
    private static final int SERVICE_TIME_MINUTES = 15;

    public List<OrderResponse> getPendingOrders() {
        return orderRepository.findByStatus(OrderStatus.PENDING).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getConfirmedOrders() {
        return orderRepository.findByStatus(OrderStatus.CONFIRMED).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllUnassignedOrders() {
        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Order> confirmed = orderRepository.findByStatus(OrderStatus.CONFIRMED);

        List<Order> all = new ArrayList<>();
        all.addAll(pending);
        all.addAll(confirmed);

        return all.stream()
                .filter(o -> o.getDriver() == null)
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zlecenia o ID: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Można zatwierdzić tylko zlecenia oczekujące");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(java.time.LocalDateTime.now());
        order = orderRepository.save(order);

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse assignOrderToDriver(Long orderId, Long driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zlecenia o ID: " + orderId));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kierowcy o ID: " + driverId));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new RuntimeException("Użytkownik nie jest kierowcą");
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Nie można przypisać anulowanego lub zakończonego zlecenia");
        }

        order.setDriver(driver);
        order.setStatus(OrderStatus.ASSIGNED);
        order = orderRepository.save(order);

        return mapToOrderResponse(order);
    }

    // ========== POBIERANIE DOSTĘPNYCH ZASOBÓW ==========

    /**
     * Zwraca kierowców dostępnych w danym dniu (mają grafik i nie mają trasy)
     */
    public List<User> getAvailableDriversForDate(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // Znajdź kierowców z aktywnym grafikiem na dany dzień
        List<DriverSchedule> activeSchedules = scheduleRepository.findByActiveTrue().stream()
                .filter(s -> s.getWorkDays() != null && s.getWorkDays().contains(dayOfWeek))
                .collect(Collectors.toList());

        // Filtruj tych, którzy nie mają jeszcze trasy tego dnia
        return activeSchedules.stream()
                .map(DriverSchedule::getDriver)
                .filter(driver -> !routeRepository.existsByDriverAndRouteDateAndStatusNot(
                        driver, date, RouteStatus.CANCELLED))
                .collect(Collectors.toList());
    }

    /**
     * Zwraca pojazdy dostępne w danym dniu (nie mają trasy)
     */
    public List<Vehicle> getAvailableVehiclesForDate(LocalDate date) {
        return vehicleRepository.findByAvailableTrue().stream()
                .filter(vehicle -> !routeRepository.existsByVehicleAndRouteDateAndStatusNot(
                        vehicle, date, RouteStatus.CANCELLED))
                .collect(Collectors.toList());
    }

    /**
     * Zwraca pojazdy dostępne w danym dniu i pasujące do wymagań zleceń
     */
    public List<Vehicle> getAvailableVehiclesForDateAndOrders(LocalDate date, List<Long> orderIds) {
        // Pobierz zlecenia i znajdź maksymalny wymagany typ pojazdu
        VehicleType requiredType = orderIds.stream()
                .map(id -> orderRepository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Order::getVehicleType)
                .max(Comparator.comparingInt(VehicleType::ordinal))
                .orElse(VehicleType.SMALL_VAN);

        return getAvailableVehiclesForDate(date).stream()
                .filter(v -> v.getType().ordinal() >= requiredType.ordinal())
                .collect(Collectors.toList());
    }

    // ========== PLANOWANIE TRASY ==========

    /**
     * Planuje trasę z ręcznym wyborem daty, kierowcy i pojazdu
     */
    @Transactional
    public RouteResponse planRoute(PlanRouteRequest request) {
        // Walidacja podstawowa
        if (request.getRouteDate() == null) {
            throw new RuntimeException("Data trasy jest wymagana");
        }
        if (request.getDriverId() == null) {
            throw new RuntimeException("Kierowca jest wymagany");
        }
        if (request.getVehicleId() == null) {
            throw new RuntimeException("Pojazd jest wymagany");
        }
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new RuntimeException("Lista zleceń nie może być pusta");
        }

        LocalDate routeDate = request.getRouteDate();
        DayOfWeek dayOfWeek = routeDate.getDayOfWeek();

        // Pobierz kierowcę
        User driver = userRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kierowcy o ID: " + request.getDriverId()));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new RuntimeException("Wybrany użytkownik nie jest kierowcą");
        }

        // Sprawdź czy kierowca ma grafik na ten dzień
        Optional<DriverSchedule> driverSchedule = scheduleRepository.findByDriverAndActiveTrue(driver);
        if (driverSchedule.isEmpty()) {
            throw new RuntimeException("Kierowca nie ma aktywnego grafiku pracy");
        }
        if (!driverSchedule.get().getWorkDays().contains(dayOfWeek)) {
            throw new RuntimeException("Kierowca nie pracuje w wybranym dniu tygodnia");
        }

        // Sprawdź czy kierowca nie ma już trasy tego dnia
        if (routeRepository.existsByDriverAndRouteDateAndStatusNot(driver, routeDate, RouteStatus.CANCELLED)) {
            throw new RuntimeException("Kierowca ma już zaplanowaną trasę na ten dzień");
        }

        // Pobierz pojazd
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono pojazdu o ID: " + request.getVehicleId()));

        if (!vehicle.getAvailable()) {
            throw new RuntimeException("Wybrany pojazd jest niedostępny");
        }

        // Sprawdź czy pojazd nie ma już trasy tego dnia
        if (routeRepository.existsByVehicleAndRouteDateAndStatusNot(vehicle, routeDate, RouteStatus.CANCELLED)) {
            throw new RuntimeException("Pojazd ma już zaplanowaną trasę na ten dzień");
        }

        // Pobierz zlecenia
        List<Order> orders = request.getOrderIds().stream()
                .map(id -> orderRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Nie znaleziono zlecenia o ID: " + id)))
                .collect(Collectors.toList());

        // Sprawdź statusy zleceń
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PENDING) {
                throw new RuntimeException("Zlecenie #" + order.getId() + " ma niewłaściwy status do planowania");
            }
            if (order.getRoute() != null) {
                throw new RuntimeException("Zlecenie #" + order.getId() + " jest już przypisane do innej trasy");
            }
        }

        // Sprawdź czy pojazd pasuje do wymagań zleceń
        VehicleType maxRequired = orders.stream()
                .map(Order::getVehicleType)
                .max(Comparator.comparingInt(VehicleType::ordinal))
                .orElse(VehicleType.SMALL_VAN);

        if (vehicle.getType().ordinal() < maxRequired.ordinal()) {
            throw new RuntimeException("Wybrany pojazd (" + vehicle.getType() +
                    ") jest za mały dla zleceń wymagających " + maxRequired);
        }

        // Optymalizuj kolejność zleceń
        List<Order> optimizedOrders = nearestNeighborAlgorithm(orders);

        // Oblicz dystans i czas
        double totalDistance = calculateTotalDistance(optimizedOrders);
        int estimatedTime = calculateEstimatedTime(totalDistance, optimizedOrders.size());

        // Utwórz trasę
        Route route = Route.builder()
                .driver(driver)
                .vehicle(vehicle)
                .routeDate(routeDate)
                .orders(optimizedOrders)
                .totalDistance(totalDistance)
                .estimatedTimeMinutes(estimatedTime)
                .status(RouteStatus.PLANNED)
                .build();

        route = routeRepository.save(route);

        // Przypisz zlecenia do trasy
        for (int i = 0; i < optimizedOrders.size(); i++) {
            Order order = optimizedOrders.get(i);
            order.setStatus(OrderStatus.ASSIGNED);
            order.setDriver(driver);
            order.setRoute(route);
            order.setOrderSequence(i);
            orderRepository.save(order);
        }

        log.info("Utworzono trasę #{} na dzień {} dla kierowcy {} z pojazdem {}, {} zleceń",
                route.getId(), routeDate, driver.getEmail(), vehicle.getRegistrationNumber(), orders.size());

        return mapToRouteResponse(route);
    }

    /**
     * Algorytm najbliższego sąsiada (Nearest Neighbor) dla problemu TSP.
     */
    private List<Order> nearestNeighborAlgorithm(List<Order> orders) {
        if (orders.size() <= 1) {
            return new ArrayList<>(orders);
        }

        List<Order> remaining = new ArrayList<>(orders);
        List<Order> optimized = new ArrayList<>();

        Order current = remaining.remove(0);
        optimized.add(current);

        while (!remaining.isEmpty()) {
            Order nearest = findNearestOrder(current, remaining);
            optimized.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }

        return optimized;
    }

    private Order findNearestOrder(Order current, List<Order> candidates) {
        Order nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Order candidate : candidates) {
            double distance = calculateDistance(
                    current.getDeliveryLocation(),
                    candidate.getPickupLocation());

            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private double calculateDistance(String location1, String location2) {
        if (location1.equalsIgnoreCase(location2)) {
            return 5.0;
        }

        long hash1 = Math.abs((long) location1.toLowerCase().hashCode());
        long hash2 = Math.abs((long) location2.toLowerCase().hashCode());

        double baseDistance = ((hash1 + hash2) % 190) + 10;
        return Math.round(baseDistance * 10.0) / 10.0;
    }

    private double calculateTotalDistance(List<Order> orders) {
        if (orders.isEmpty())
            return 0;

        double total = 0;

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            total += calculateDistance(order.getPickupLocation(), order.getDeliveryLocation());

            if (i < orders.size() - 1) {
                Order next = orders.get(i + 1);
                total += calculateDistance(order.getDeliveryLocation(), next.getPickupLocation());
            }
        }

        return Math.round(total * 10.0) / 10.0;
    }

    private int calculateEstimatedTime(double distanceKm, int numberOfStops) {
        double travelTimeHours = distanceKm / AVERAGE_SPEED_KMH;
        int travelTimeMinutes = (int) Math.round(travelTimeHours * 60);
        int serviceTime = numberOfStops * 2 * SERVICE_TIME_MINUTES;
        return travelTimeMinutes + serviceTime;
    }

    // ========== ZARZĄDZANIE TRASAMI ==========

    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::mapToRouteResponse)
                .collect(Collectors.toList());
    }

    public List<RouteResponse> getRoutesByDate(LocalDate date) {
        return routeRepository.findByRouteDate(date).stream()
                .map(this::mapToRouteResponse)
                .collect(Collectors.toList());
    }

    public RouteResponse getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono trasy o ID: " + id));
        return mapToRouteResponse(route);
    }

    @Transactional
    public RouteResponse cancelRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono trasy o ID: " + routeId));

        if (route.getStatus() == RouteStatus.COMPLETED) {
            throw new RuntimeException("Nie można anulować zakończonej trasy");
        }
        if (route.getStatus() == RouteStatus.CANCELLED) {
            throw new RuntimeException("Trasa jest już anulowana");
        }
        if (route.getStatus() == RouteStatus.IN_PROGRESS) {
            throw new RuntimeException("Nie można anulować trasy w trakcie realizacji");
        }

        // Przywróć zlecenia do statusu CONFIRMED
        for (Order order : route.getOrders()) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setDriver(null);
            order.setRoute(null);
            order.setOrderSequence(null);
            orderRepository.save(order);
        }

        route.setStatus(RouteStatus.CANCELLED);
        route = routeRepository.save(route);

        log.info("Anulowano trasę #{}, {} zleceń przywrócono do planowania", routeId, route.getOrders().size());

        return mapToRouteResponse(route);
    }

    // ========== MAPOWANIA ==========

    private OrderResponse mapToOrderResponse(Order order) {
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
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private RouteResponse mapToRouteResponse(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .driverId(route.getDriver().getId())
                .driverEmail(route.getDriver().getEmail())
                .vehicleId(route.getVehicle().getId())
                .vehicleRegistration(route.getVehicle().getRegistrationNumber())
                .routeDate(route.getRouteDate())
                .orders(route.getOrders().stream()
                        .map(this::mapToOrderResponse)
                        .collect(Collectors.toList()))
                .totalDistance(route.getTotalDistance())
                .estimatedTimeMinutes(route.getEstimatedTimeMinutes())
                .status(route.getStatus())
                .createdAt(route.getCreatedAt())
                .build();
    }
}
