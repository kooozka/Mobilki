package com.example.demo.dispatch.service;

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
import java.time.LocalDateTime;
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
        order.setConfirmedAt(LocalDateTime.now());
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

    /**
     * Planuje trasy dla wybranych zleceń używając algorytmu Nearest Neighbor (najbliższego sąsiada).
     * Jest to prosty algorytm heurystyczny do rozwiązywania problemu TSP.
     */
    @Transactional
    public List<RouteResponse> planRoutes(List<Long> orderIds) {
        // Pobierz zlecenia
        List<Order> orders = orderIds.stream()
                .map(id -> orderRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Nie znaleziono zlecenia o ID: " + id)))
                .collect(Collectors.toList());
        
        // Sprawdź czy wszystkie zlecenia są potwierdzone
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PENDING) {
                throw new RuntimeException("Zlecenie " + order.getId() + " ma niewłaściwy status do planowania");
            }
        }
        
        // Grupuj zlecenia według typu pojazdu
        Map<VehicleType, List<Order>> ordersByVehicleType = orders.stream()
                .collect(Collectors.groupingBy(Order::getVehicleType));
        
        List<Route> routes = new ArrayList<>();
        LocalDate routeDate = determineRouteDate(orders);
        DayOfWeek dayOfWeek = routeDate.getDayOfWeek();
        
        // Dla każdego typu pojazdu znajdź dostępnych kierowców i zaplanuj trasę
        for (Map.Entry<VehicleType, List<Order>> entry : ordersByVehicleType.entrySet()) {
            VehicleType vehicleType = entry.getKey();
            List<Order> vehicleOrders = entry.getValue();
            
            // Znajdź kierowców z odpowiednim pojazdem i grafiku na dany dzień
            List<DriverSchedule> availableSchedules = scheduleRepository.findByActiveTrue().stream()
                    .filter(s -> s.getWorkDays() != null && s.getWorkDays().contains(dayOfWeek))
                    .filter(s -> s.getAssignedVehicle() != null && s.getAssignedVehicle().getType() == vehicleType)
                    .collect(Collectors.toList());
            
            if (availableSchedules.isEmpty()) {
                log.warn("Brak dostępnych kierowców dla typu pojazdu: " + vehicleType);
                // Przypisz do pierwszego dostępnego kierowcy z jakimkolwiek pojazdem tego typu
                List<Vehicle> vehicles = vehicleRepository.findByAvailableTrueAndType(vehicleType);
                if (vehicles.isEmpty()) {
                    throw new RuntimeException("Brak dostępnych pojazdów typu: " + vehicleType);
                }
                
                List<User> drivers = userRepository.findByRole(UserRole.DRIVER);
                if (drivers.isEmpty()) {
                    throw new RuntimeException("Brak zarejestrowanych kierowców");
                }
                
                // Utwórz trasę z pierwszym dostępnym kierowcą i pojazdem
                Route route = createOptimizedRoute(vehicleOrders, drivers.get(0), vehicles.get(0), routeDate);
                routes.add(route);
            } else {
                // Rozdziel zlecenia między dostępnych kierowców
                int ordersPerDriver = (int) Math.ceil((double) vehicleOrders.size() / availableSchedules.size());
                
                for (int i = 0; i < availableSchedules.size() && !vehicleOrders.isEmpty(); i++) {
                    DriverSchedule schedule = availableSchedules.get(i);
                    
                    List<Order> driverOrders = vehicleOrders.subList(0, 
                            Math.min(ordersPerDriver, vehicleOrders.size()));
                    
                    Route route = createOptimizedRoute(
                            new ArrayList<>(driverOrders), 
                            schedule.getDriver(), 
                            schedule.getAssignedVehicle(), 
                            routeDate
                    );
                    routes.add(route);
                    
                    vehicleOrders = new ArrayList<>(vehicleOrders.subList(
                            Math.min(ordersPerDriver, vehicleOrders.size()), 
                            vehicleOrders.size()
                    ));
                }
            }
        }
        
        // Zapisz trasy i zaktualizuj statusy zleceń
        List<Route> savedRoutes = new ArrayList<>();
        for (Route route : routes) {
            route = routeRepository.save(route);
            
            // Zaktualizuj statusy zleceń
            for (Order order : route.getOrders()) {
                order.setStatus(OrderStatus.ASSIGNED);
                order.setDriver(route.getDriver());
                orderRepository.save(order);
            }
            
            savedRoutes.add(route);
        }
        
        return savedRoutes.stream()
                .map(this::mapToRouteResponse)
                .collect(Collectors.toList());
    }

    /**
     * Algorytm Nearest Neighbor - optymalizuje kolejność punktów na trasie.
     */
    private Route createOptimizedRoute(List<Order> orders, User driver, Vehicle vehicle, LocalDate routeDate) {
        if (orders.isEmpty()) {
            throw new RuntimeException("Lista zleceń nie może być pusta");
        }
        
        // Używamy algorytmu Nearest Neighbor do optymalizacji kolejności
        List<Order> optimizedOrders = nearestNeighborAlgorithm(orders);
        
        // Oblicz całkowitą odległość i czas
        double totalDistance = calculateTotalDistance(optimizedOrders);
        int estimatedTime = calculateEstimatedTime(totalDistance, optimizedOrders.size());
        
        return Route.builder()
                .driver(driver)
                .vehicle(vehicle)
                .routeDate(routeDate)
                .orders(optimizedOrders)
                .totalDistance(totalDistance)
                .estimatedTimeMinutes(estimatedTime)
                .status(RouteStatus.PLANNED)
                .build();
    }

    /**
     * Algorytm najbliższego sąsiada (Nearest Neighbor) dla problemu TSP.
     * Zaczyna od pierwszego punktu i zawsze wybiera najbliższy nieodwiedzony punkt.
     */
    private List<Order> nearestNeighborAlgorithm(List<Order> orders) {
        if (orders.size() <= 1) {
            return new ArrayList<>(orders);
        }
        
        List<Order> remaining = new ArrayList<>(orders);
        List<Order> optimized = new ArrayList<>();
        
        // Zacznij od pierwszego zlecenia
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

    /**
     * Znajduje najbliższe zlecenie do obecnego punktu.
     */
    private Order findNearestOrder(Order current, List<Order> candidates) {
        Order nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Order candidate : candidates) {
            // Oblicz odległość od punktu dostawy obecnego zlecenia do punktu odbioru następnego
            double distance = calculateDistance(
                    current.getDeliveryLocation(),
                    candidate.getPickupLocation()
            );
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }
        
        return nearest;
    }

    /**
     * Prosta symulacja odległości między miastami.
     * W rzeczywistości użyjemy API Google Maps
     */
    private double calculateDistance(String location1, String location2) {
        // Dla uproszczenia: losowa odległość bazująca na hashach nazw miast
        // W rzeczywistej aplikacji skorzystamy z API map
        if (location1.equalsIgnoreCase(location2)) {
            return 5.0; // Zlecenia w tym samym mieście
        }
        
        // Używamy long aby uniknąć integer overflow przy dodawaniu
        long hash1 = Math.abs((long) location1.toLowerCase().hashCode());
        long hash2 = Math.abs((long) location2.toLowerCase().hashCode());
        
        // Generuj "pseudo-losową" ale deterministyczną odległość między 10 a 200 km
        double baseDistance = ((hash1 + hash2) % 190) + 10;
        return Math.round(baseDistance * 10.0) / 10.0;
    }

    /**
     * Oblicza całkowitą odległość trasy.
     */
    private double calculateTotalDistance(List<Order> orders) {
        if (orders.isEmpty()) return 0;
        
        double total = 0;
        
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            
            // Odległość wewnątrz zlecenia (odbiór -> dostawa)
            total += calculateDistance(order.getPickupLocation(), order.getDeliveryLocation());
            
            // Odległość do następnego zlecenia
            if (i < orders.size() - 1) {
                Order next = orders.get(i + 1);
                total += calculateDistance(order.getDeliveryLocation(), next.getPickupLocation());
            }
        }
        
        return Math.round(total * 10.0) / 10.0;
    }

    /**
     * Oblicza szacowany czas trasy w minutach.
     */
    private int calculateEstimatedTime(double distanceKm, int numberOfStops) {
        // Czas przejazdu
        double travelTimeHours = distanceKm / AVERAGE_SPEED_KMH;
        int travelTimeMinutes = (int) Math.round(travelTimeHours * 60);
        
        // Czas obsługi punktów (odbiór + dostawa dla każdego zlecenia)
        int serviceTime = numberOfStops * 2 * SERVICE_TIME_MINUTES;
        
        return travelTimeMinutes + serviceTime;
    }

    /**
     * Określa datę trasy na podstawie zleceń.
     */
    private LocalDate determineRouteDate(List<Order> orders) {
        // Znajdź najwcześniejszą datę odbioru
        return orders.stream()
                .map(Order::getPickupDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now().plusDays(1));
    }

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

    /**
     * Anuluje trasę i przywraca zlecenia do statusu CONFIRMED (do ponownego zaplanowania).
     */
    @Transactional
    public RouteResponse cancelRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono trasy o ID: " + routeId));
        
        // Sprawdź czy trasę można anulować
        if (route.getStatus() == RouteStatus.COMPLETED) {
            throw new RuntimeException("Nie można anulować zakończonej trasy");
        }
        if (route.getStatus() == RouteStatus.CANCELLED) {
            throw new RuntimeException("Trasa jest już anulowana");
        }
        if (route.getStatus() == RouteStatus.IN_PROGRESS) {
            throw new RuntimeException("Nie można anulować trasy w trakcie realizacji");
        }
        
        // Przywróć zlecenia do statusu CONFIRMED i usuń przypisanie kierowcy
        for (Order order : route.getOrders()) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setDriver(null);
            orderRepository.save(order);
        }
        
        // Oznacz trasę jako anulowaną
        route.setStatus(RouteStatus.CANCELLED);
        route = routeRepository.save(route);
        
        log.info("Anulowano trasę #{}, {} zleceń przywrócono do planowania", routeId, route.getOrders().size());
        
        return mapToRouteResponse(route);
    }

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
