package com.example.demo.order.model;

import com.example.demo.security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private User client;
    
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    @Column(nullable = false)
    private String title = "";

    @Column(nullable = false)
    private double price = 0.0;
    
    // Punkt odbioru
    @Column(nullable = false)
    private String pickupLocation;
    
    @Column(nullable = false)
    private String pickupAddress;
    
    @Column(nullable = false)
    private LocalDateTime pickupTimeFrom;
    
    @Column(nullable = false)
    private LocalDateTime pickupTimeTo;
    
    // Punkt dostawy
    @Column(nullable = false)
    private String deliveryLocation;
    
    @Column(nullable = false)
    private String deliveryAddress;
    
    @Column(nullable = false)
    private LocalDateTime deliveryTimeFrom;
    
    @Column(nullable = false)
    private LocalDateTime deliveryTimeTo;
    
    // Szczegóły ładunku
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;
    
    @Column(nullable = false)
    private Double cargoWeight; // w tonach
    
    @Column(length = 1000)
    private String description;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime confirmedAt;
    
    @Column
    private LocalDateTime cancelledAt;
    
    @Column
    private String cancellationReason;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
