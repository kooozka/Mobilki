package com.example.demo.order.service;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User client = getCurrentUser();

        // Konwersja vehicleType ze stringa na enum
        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(request.getVehicleType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Nieprawidłowy typ pojazdu: " + request.getVehicleType());
        }

        // Walidacja dat
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        if (request.getPickupTimeFrom().isBefore(tomorrow)) {
            throw new RuntimeException("Data odbioru nie może być wcześniejsza niż jutro. " +
                "Najwcześniejsza możliwa data: " + tomorrow.toLocalDate());
        }
        
        if (request.getPickupTimeFrom().isAfter(request.getPickupTimeTo())) {
            throw new RuntimeException("Nieprawidłowy zakres czasowy odbioru: data rozpoczęcia (" + 
                request.getPickupTimeFrom() + ") jest późniejsza niż data zakończenia (" + 
                request.getPickupTimeTo() + ")");
        }
        if (request.getDeliveryTimeFrom().isAfter(request.getDeliveryTimeTo())) {
            throw new RuntimeException("Nieprawidłowy zakres czasowy dostawy: data rozpoczęcia (" + 
                request.getDeliveryTimeFrom() + ") jest późniejsza niż data zakończenia (" + 
                request.getDeliveryTimeTo() + ")");
        }
        if (request.getPickupTimeTo().isAfter(request.getDeliveryTimeFrom())) {
            throw new RuntimeException("Odbiór musi zostać zakończony przed rozpoczęciem dostawy. " +
                "Koniec okna odbioru: " + request.getPickupTimeTo() + 
                ", Początek okna dostawy: " + request.getDeliveryTimeFrom());
        }

        // Walidacja wagi
        if (request.getCargoWeight() > vehicleType.getMaxWeight()) {
            throw new RuntimeException("Waga ładunku (" + request.getCargoWeight() + 
                " kg) przekracza maksymalną ładowność pojazdu " + vehicleType + 
                " (" + vehicleType.getMaxWeight() + " kg)");
        }

        Order order = Order.builder()
                .client(client)
                .title(request.getTitle())
                .price(calculatePrice(request))
                .pickupLocation(request.getPickupLocation())
                .pickupAddress(request.getPickupAddress())
                .pickupTimeFrom(request.getPickupTimeFrom())
                .pickupTimeTo(request.getPickupTimeTo())
                .deliveryLocation(request.getDeliveryLocation())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryTimeFrom(request.getDeliveryTimeFrom())
                .deliveryTimeTo(request.getDeliveryTimeTo())
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
            throw new RuntimeException("Nie można anulować zlecenia - jest w trakcie realizacji. Skontaktuj się z dyspozytorem.");
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

        // Konwersja vehicleType ze stringa na enum
        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(request.getVehicleType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Nieprawidłowy typ pojazdu: " + request.getVehicleType());
        }

        // Walidacja dat
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        if (request.getPickupTimeFrom().isBefore(tomorrow)) {
            throw new RuntimeException("Data odbioru nie może być wcześniejsza niż jutro. " +
                "Najwcześniejsza możliwa data: " + tomorrow.toLocalDate());
        }
        
        if (request.getPickupTimeFrom().isAfter(request.getPickupTimeTo())) {
            throw new RuntimeException("Nieprawidłowy zakres czasowy odbioru: data rozpoczęcia (" + 
                request.getPickupTimeFrom() + ") jest późniejsza niż data zakończenia (" + 
                request.getPickupTimeTo() + ")");
        }
        if (request.getDeliveryTimeFrom().isAfter(request.getDeliveryTimeTo())) {
            throw new RuntimeException("Nieprawidłowy zakres czasowy dostawy: data rozpoczęcia (" + 
                request.getDeliveryTimeFrom() + ") jest późniejsza niż data zakończenia (" + 
                request.getDeliveryTimeTo() + ")");
        }
        if (request.getPickupTimeTo().isAfter(request.getDeliveryTimeFrom())) {
            throw new RuntimeException("Odbiór musi zostać zakończony przed rozpoczęciem dostawy. " +
                "Koniec okna odbioru: " + request.getPickupTimeTo() + 
                ", Początek okna dostawy: " + request.getDeliveryTimeFrom());
        }

        // Walidacja wagi
        if (request.getCargoWeight() > vehicleType.getMaxWeight()) {
            throw new RuntimeException("Waga ładunku (" + request.getCargoWeight() + 
                " kg) przekracza maksymalną ładowność pojazdu " + vehicleType + 
                " (" + vehicleType.getMaxWeight() + " kg)");
        }

        // Aktualizacja pól zlecenia
        order.setPickupLocation(request.getPickupLocation());
        order.setPickupAddress(request.getPickupAddress());
        order.setPickupTimeFrom(request.getPickupTimeFrom());
        order.setPickupTimeTo(request.getPickupTimeTo());
        order.setDeliveryLocation(request.getDeliveryLocation());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryTimeFrom(request.getDeliveryTimeFrom());
        order.setDeliveryTimeTo(request.getDeliveryTimeTo());
        order.setVehicleType(vehicleType);
        order.setCargoWeight(request.getCargoWeight());
        order.setDescription(request.getDescription());

        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .title(order.getTitle())
                .clientEmail(order.getClient().getEmail())
                .driverEmail(order.getDriver() != null ? order.getDriver().getEmail() : null)
                .pickupLocation(order.getPickupLocation())
                .pickupAddress(order.getPickupAddress())
                .pickupTimeFrom(order.getPickupTimeFrom())
                .pickupTimeTo(order.getPickupTimeTo())
                .deliveryLocation(order.getDeliveryLocation())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryTimeFrom(order.getDeliveryTimeFrom())
                .deliveryTimeTo(order.getDeliveryTimeTo())
                .vehicleType(order.getVehicleType())
                .cargoWeight(order.getCargoWeight())
                .description(order.getDescription())
                .status(order.getStatus())
                .price(order.getPrice())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .confirmedAt(order.getConfirmedAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .build();
    }

    private double calculatePrice(CreateOrderRequest createOrderRequest) {
        // na razie uwzględnij tylko masą ładunku i typ pojazdu
        double baseRate;
        switch (createOrderRequest.getVehicleType()) {
            case "SMALL_VAN":
                baseRate = 1.0;
                break;
            case "MEDIUM_TRUCK":
                baseRate = 1.5;
                break;
            case "LARGE_TRUCK":
                baseRate = 2.0;
                break;
            case "SEMI_TRUCK":
                baseRate = 3.0;
                break;
            default:
                throw new RuntimeException("Nieprawidłowy typ pojazdu: " + createOrderRequest.getVehicleType());
        }
        return baseRate * createOrderRequest.getCargoWeight();
    }
}
