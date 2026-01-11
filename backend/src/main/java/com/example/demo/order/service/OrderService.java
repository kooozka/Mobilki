package com.example.demo.order.service;

import com.example.demo.dispatch.service.GoogleMapsService;
import com.example.demo.order.dto.CancelOrderRequest;
import com.example.demo.order.dto.CreateOrderRequest;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.model.Order;
import com.example.demo.order.model.OrderStatus;
import com.example.demo.security.model.User;
import com.example.demo.order.model.VehicleType;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final GoogleMapsService googleMapsService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User client = getCurrentUser();

        // Walidacja wagi - maksymalnie 25000 kg
        if (request.getCargoWeight() > 25000) {
            throw new RuntimeException("Maksymalna waga ładunku to 25000 kg");
        }

        // Walidacja adresów przez Google Maps
        validateAddresses(request.getPickupLocation(), request.getDeliveryLocation());

        // Automatyczne przypisanie najmniejszego możliwego pojazdu na podstawie wagi
        VehicleType vehicleType = determineVehicleType(request.getCargoWeight());

        // Walidacja dat - używamy LocalDate z requestu
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (request.getPickupDate().isBefore(tomorrow)) {
            throw new RuntimeException("Data odbioru nie może być wcześniejsza niż jutro. " +
                    "Najwcześniejsza możliwa data: " + tomorrow);
        }

        if (request.getPickupDate().isAfter(request.getDeliveryDeadline())) {
            throw new RuntimeException("Data odbioru nie może być późniejsza niż termin dostawy");
        }

        Order order = Order.builder()
                .client(client)
                .title(request.getTitle())
                .pickupLocation(request.getPickupLocation())
                .pickupAddress(request.getPickupAddress())
                .pickupDate(request.getPickupDate())
                .deliveryLocation(request.getDeliveryLocation())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryDeadline(request.getDeliveryDeadline())
                .vehicleType(vehicleType)
                .cargoWeight(request.getCargoWeight())
                .description(request.getDescription())
                .status(OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    public List<OrderResponse> getMyOrders() {
        User client = getCurrentUser();
        return orderRepository.findByClientOrderByCreatedAtDesc(client)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long orderId) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Sprawdź czy użytkownik ma dostęp do zlecenia
        if (!order.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, CancelOrderRequest request) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zlecenia o ID: " + orderId));

        // Sprawdź czy użytkownik ma dostęp do zlecenia
        if (!order.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Brak uprawnień do anulowania tego zlecenia");
        }

        // Sprawdź czy zlecenie może być anulowane
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Nie można anulować zlecenia - jest już zakończone");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Zlecenie jest już anulowane");
        }
        if (order.getStatus() == OrderStatus.IN_PROGRESS) {
            throw new RuntimeException(
                    "Nie można anulować zlecenia - jest w trakcie realizacji. Skontaktuj się z dyspozytorem.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(request.getReason());

        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse updateOrder(Long orderId, CreateOrderRequest request) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zlecenia o ID: " + orderId));

        // Sprawdź czy użytkownik ma dostęp do zlecenia
        if (!order.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Brak uprawnień do modyfikacji tego zlecenia");
        }

        // Sprawdź czy zlecenie może być modyfikowane
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Można modyfikować tylko zlecenia o statusie OCZEKUJĄCE lub POTWIERDZONE");
        }

        // Walidacja wagi - maksymalnie 25000 kg
        if (request.getCargoWeight() > 25000) {
            throw new RuntimeException("Maksymalna waga ładunku to 25000 kg");
        }

        // Walidacja adresów przez Google Maps
        validateAddresses(request.getPickupLocation(), request.getDeliveryLocation());

        // Automatyczne przypisanie najmniejszego możliwego pojazdu na podstawie wagi
        VehicleType vehicleType = determineVehicleType(request.getCargoWeight());

        // Walidacja dat - używamy LocalDate z requestu
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (request.getPickupDate().isBefore(tomorrow)) {
            throw new RuntimeException("Data odbioru nie może być wcześniejsza niż jutro. " +
                    "Najwcześniejsza możliwa data: " + tomorrow);
        }

        if (request.getPickupDate().isAfter(request.getDeliveryDeadline())) {
            throw new RuntimeException("Data odbioru nie może być późniejsza niż termin dostawy");
        }

        // Aktualizacja pól zlecenia
        order.setTitle(request.getTitle());
        order.setPickupLocation(request.getPickupLocation());
        order.setPickupAddress(request.getPickupAddress());
        order.setPickupDate(request.getPickupDate());
        order.setDeliveryLocation(request.getDeliveryLocation());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryDeadline(request.getDeliveryDeadline());
        order.setVehicleType(vehicleType);
        order.setCargoWeight(request.getCargoWeight());
        order.setDescription(request.getDescription());

        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    /**
     * Automatycznie dobiera najmniejszy możliwy pojazd na podstawie wagi ładunku.
     */
    private VehicleType determineVehicleType(Double cargoWeight) {
        for (VehicleType type : VehicleType.values()) {
            if (cargoWeight <= type.getMaxWeight()) {
                return type;
            }
        }
        // Jeśli waga przekracza wszystkie pojazdy, zwróć największy
        return VehicleType.SEMI_TRUCK;
    }

    /**
     * Waliduje adresy odbioru i dostawy przez Google Maps Geocoding API.
     */
    private void validateAddresses(String pickupLocation, String deliveryLocation) {
        // Walidacja adresu odbioru
        GoogleMapsService.AddressValidationResult pickupResult = googleMapsService.geocodeAddress(pickupLocation);
        if (!pickupResult.isValid()) {
            log.warn("Invalid pickup address: {} - {}", pickupLocation, pickupResult.getErrorMessage());
            throw new RuntimeException("Nieprawidłowy adres odbioru: " + pickupLocation +
                    ". " + pickupResult.getErrorMessage());
        }
        log.info("Pickup address validated: {} -> {}", pickupLocation, pickupResult.getFormattedAddress());

        // Walidacja adresu dostawy
        GoogleMapsService.AddressValidationResult deliveryResult = googleMapsService.geocodeAddress(deliveryLocation);
        if (!deliveryResult.isValid()) {
            log.warn("Invalid delivery address: {} - {}", deliveryLocation, deliveryResult.getErrorMessage());
            throw new RuntimeException("Nieprawidłowy adres dostawy: " + deliveryLocation +
                    ". " + deliveryResult.getErrorMessage());
        }
        log.info("Delivery address validated: {} -> {}", deliveryLocation, deliveryResult.getFormattedAddress());
    }

    private OrderResponse mapToResponse(Order order) {
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
                .confirmedAt(order.getConfirmedAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .build();
    }
}
