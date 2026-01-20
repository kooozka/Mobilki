package com.example.demo.dispatch.service;

import com.example.demo.dispatch.dto.RouteResponse;
import com.example.demo.dispatch.model.*;
import com.example.demo.dispatch.repository.DriverScheduleRepository;
import com.example.demo.dispatch.repository.RouteRepository;
import com.example.demo.dispatch.repository.VehicleRepository;
import com.example.demo.order.model.Order;
import com.example.demo.order.model.OrderStatus;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serwis do automatycznego planowania tras.
 * 
 * Algorytm:
 * 1. Grupuj zlecenia według wymaganego typu pojazdu
 * 2. Dla każdej grupy znajdź dostępnych kierowców i pojazdy
 * 3. Przypisz zlecenia do par (kierowca, pojazd) optymalizując kolejność
 * 4. Ograniczenie: jedno zlecenie na raz (pickup -> delivery -> następne)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPlanningService {

    private final OrderRepository orderRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverScheduleRepository scheduleRepository;
    private final GoogleMapsService googleMapsService;
    private final RoutePlanningService routePlanningService;

    private static final int SERVICE_TIME_MINUTES = 15;

    /**
     * Automatycznie planuje trasy dla podanych zleceń na wybrany dzień.
     */
    @Transactional
    public List<RouteResponse> autoPlanRoutes(List<Long> orderIds, LocalDate routeDate) {
        log.info("Auto-planning routes for {} orders on {}", orderIds.size(), routeDate);

        // Pobierz zlecenia
        List<Order> orders = orderRepository.findAllById(orderIds);

        // Walidacja (sprawdza status, datę odbioru = routeDate, deadline >= routeDate)
        validateOrders(orders, orderIds, routeDate);

        List<Driver> availableDrivers = getAvailableDrivers(routeDate);

        List<Vehicle> availableVehicles = getAvailableVehicles(routeDate);

        // Algorytm przypisywania
        List<RouteResponse> createdRoutes = new ArrayList<>();
        List<Order> remainingOrders = new ArrayList<>(orders);

        // Sortuj pojazdy od największej ładowności
        availableVehicles.sort((a, b) -> Double.compare(b.getMaxWeight(), a.getMaxWeight()));

        int driverIndex = 0;
        int vehicleIndex = 0;

        while (!remainingOrders.isEmpty() && driverIndex < availableDrivers.size()) {
            Driver driver = availableDrivers.get(driverIndex);

            // Znajdź odpowiedni pojazd dla pozostałych zleceń
            Vehicle vehicle = findSuitableVehicle(remainingOrders, availableVehicles, vehicleIndex);

            if (vehicle == null) {
                log.warn("Brak odpowiedniego pojazdu dla pozostałych zleceń");
                break;
            }

            // Zbierz zlecenia pasujące do tego pojazdu
            List<Order> ordersForRoute = collectOrdersForVehicle(remainingOrders, vehicle);

            if (ordersForRoute.isEmpty()) {
                vehicleIndex++;
                continue;
            }

            // Optymalizuj kolejność (Nearest Neighbor)
            List<Order> optimizedOrders = optimizeOrderSequence(ordersForRoute);

            // Oblicz dystans i czas używając Google Maps
            double[] distanceAndTime = calculateSerialRouteDistanceAndTime(optimizedOrders);
            double totalDistance = distanceAndTime[0];
            int estimatedTime = (int) distanceAndTime[1];

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

            log.info("Created route #{} for driver {} with vehicle {}, {} orders",
                    route.getId(), driver.getUser().getEmail(), vehicle.getRegistrationNumber(), optimizedOrders.size());

            createdRoutes.add(mapToRouteResponse(route));

            // Usuń przypisane zlecenia z remainingOrders
            remainingOrders.removeAll(ordersForRoute);

            // Przejdź do następnego kierowcy
            driverIndex++;

            // Usuń użyty pojazd z listy dostępnych
            availableVehicles.remove(vehicle);
        }

        if (!remainingOrders.isEmpty()) {
            log.warn("{} orders could not be assigned", remainingOrders.size());
        }

        return createdRoutes;
    }

    /**
     * Pobiera listę dostępnych harmonogramów kierowców na dany dzień.
     */
    protected List<DriverSchedule> getAvailableSchedules(LocalDate routeDate) {
        DayOfWeek dayOfWeek = routeDate.getDayOfWeek();
        List<DriverSchedule> availableSchedules = scheduleRepository.findByWorkDaysContainingAndActiveTrue(dayOfWeek);

        if (availableSchedules.isEmpty()) {
            throw new RuntimeException("Brak dostępnych kierowców na dzień " + routeDate);
        }

        // Filtruj kierowców bez istniejących tras na ten dzień
        availableSchedules = availableSchedules.stream()
                .filter(schedule -> !routeRepository.existsByDriverAndRouteDateAndStatusNot(
                        schedule.getDriver(), routeDate, RouteStatus.CANCELLED))
                .filter(schedule -> !schedule.getDriver().getUser().isSuspended())
                .toList();

        if (availableSchedules.isEmpty()) {
            throw new RuntimeException("Wszyscy kierowcy mają już zaplanowane trasy na " + routeDate);
        }

        return availableSchedules;
    }

    /**
     * Pobiera listę dostępnych kierowców na dany dzień.
     */
     protected List<Driver> getAvailableDrivers(LocalDate routeDate) {
        List<DriverSchedule> availableSchedules = getAvailableSchedules(routeDate);

         return availableSchedules.stream()
                 .map(DriverSchedule::getDriver)
                 .toList();
     }

    /**
     * Pobiera listę dostępnych pojazdów na dany dzień.
     */
    protected List<Vehicle> getAvailableVehicles(LocalDate routeDate) {
        // Pobierz dostępne pojazdy
        List<Vehicle> availableVehicles = vehicleRepository.findByAvailableTrue().stream()
                .filter(v -> !routeRepository.existsByVehicleAndRouteDateAndStatusNot(
                        v, routeDate, RouteStatus.CANCELLED))
                .toList();

        if (availableVehicles.isEmpty()) {
            throw new RuntimeException("Brak dostępnych pojazdów na dzień " + routeDate);
        }

        return availableVehicles;
    }

    /**
     * Waliduje zlecenia przed planowaniem.
     * Sprawdza: status, przypisanie do trasy, datę odbioru i deadline.
     */
    protected void validateOrders(List<Order> orders, List<Long> requestedIds, LocalDate routeDate) {
        if (orders.size() != requestedIds.size()) {
            throw new RuntimeException("Nie znaleziono niektórych zleceń");
        }

        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                throw new RuntimeException("Zlecenie #" + order.getId() +
                        " ma nieprawidłowy status: " + order.getStatus());
            }
            if (order.getRoute() != null) {
                throw new RuntimeException("Zlecenie #" + order.getId() +
                        " jest już przypisane do trasy");
            }

            // Walidacja daty odbioru - musi być dokładnie w dniu trasy
            if (!order.getPickupDate().equals(routeDate)) {
                throw new RuntimeException("Zlecenie #" + order.getId() +
                        " ma datę odbioru " + order.getPickupDate() +
                        ", ale trasa jest planowana na " + routeDate);
            }

            // Walidacja deadline - trasa musi być przed lub w dniu deadline
            if (routeDate.isAfter(order.getDeliveryDeadline())) {
                throw new RuntimeException("Zlecenie #" + order.getId() +
                        " ma deadline " + order.getDeliveryDeadline() +
                        ", ale trasa jest planowana na " + routeDate + " (za późno!)");
            }
        }
    }

    /**
     * Znajduje odpowiedni pojazd dla zleceń.
     */
    protected Vehicle findSuitableVehicle(List<Order> orders, List<Vehicle> vehicles, int startIndex) {
        // Znajdź maksymalną wagę wśród zleceń
        double maxWeight = orders.stream()
                .mapToDouble(Order::getCargoWeight)
                .max()
                .orElse(0);

        for (int i = startIndex; i < vehicles.size(); i++) {
            if (vehicles.get(i).getMaxWeight() >= maxWeight) {
                return vehicles.get(i);
            }
        }
        return null;
    }

    /**
     * Zbiera zlecenia pasujące do ładowności pojazdu.
     */
    private List<Order> collectOrdersForVehicle(List<Order> orders, Vehicle vehicle) {
        return orders.stream()
                .filter(o -> o.getCargoWeight() <= vehicle.getMaxWeight())
                .collect(Collectors.toList());
    }

    /**
     * Optymalizuje kolejność zleceń używając algorytmu Nearest Neighbor.
     * Dla każdego zlecenia: pickup -> delivery -> następny najbliższy pickup
     */
    private List<Order> optimizeOrderSequence(List<Order> orders) {
        if (orders.size() <= 1) {
            return new ArrayList<>(orders);
        }

        List<Order> remaining = new ArrayList<>(orders);
        List<Order> optimized = new ArrayList<>();

        // Zacznij od punktu "depot" - wybierz zlecenie z najbliższym pickup
        String currentLocation = "Polska"; // Ogólna lokalizacja startowa

        while (!remaining.isEmpty()) {
            Order nearest = findNearestPickup(currentLocation, remaining);
            optimized.add(nearest);
            remaining.remove(nearest);

            // Po realizacji tego zlecenia, jesteśmy w miejscu delivery
            currentLocation = nearest.getDeliveryLocation();
        }

        log.debug("Order sequence optimized: {}",
                optimized.stream().map(o -> "#" + o.getId()).collect(Collectors.joining(" -> ")));

        return optimized;
    }

    /**
     * Znajduje zlecenie z najbliższym pickup do aktualnej lokalizacji.
     */
    private Order findNearestPickup(String currentLocation, List<Order> orders) {
        Order nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Order order : orders) {
            double distance = googleMapsService.getDistance(
                    currentLocation, order.getPickupLocation()).getDistanceKm();

            if (distance < minDistance) {
                minDistance = distance;
                nearest = order;
            }
        }

        return nearest;
    }

    /**
     * Oblicza całkowity dystans i czas dla sekwencji zleceń używając Google Maps.
     * Trasa: pickup1 -> delivery1 -> pickup2 -> delivery2 -> ...
     * 
     * @return tablica [dystans w km, czas w minutach]
     */
    private double[] calculateSerialRouteDistanceAndTime(List<Order> orders) {
        if (orders.isEmpty()) {
            return new double[] { 0, 0 };
        }

        double totalDistance = 0;
        int totalTime = 0;
        String currentLocation = orders.get(0).getPickupLocation();

        for (Order order : orders) {
            // Dojazd do pickup (jeśli nie jesteśmy już tam)
            if (!currentLocation.equals(order.getPickupLocation())) {
                GoogleMapsService.DistanceResult result = googleMapsService.getDistance(
                        currentLocation, order.getPickupLocation());
                totalDistance += result.getDistanceKm();
                totalTime += result.getDurationMinutes();
            }

            // Pickup -> Delivery
            GoogleMapsService.DistanceResult result = googleMapsService.getDistance(
                    order.getPickupLocation(), order.getDeliveryLocation());
            totalDistance += result.getDistanceKm();
            totalTime += result.getDurationMinutes();

            // Czas obsługi: pickup + delivery
            totalTime += SERVICE_TIME_MINUTES * 2;

            currentLocation = order.getDeliveryLocation();
        }

        return new double[] { Math.round(totalDistance * 10.0) / 10.0, totalTime };
    }

    /**
     * Mapuje Route do RouteResponse.
     */
    private RouteResponse mapToRouteResponse(Route route) {
        return routePlanningService.getRouteById(route.getId());
    }
}
