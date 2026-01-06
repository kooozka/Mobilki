package com.example.demo.dispatch.model;

import com.example.demo.order.model.Order;
import com.example.demo.security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private LocalDate routeDate;

    // Uproszczona relacja: jedna trasa ma wiele zleceń,
    // każde zlecenie należy do maksymalnie jednej trasy
    @OneToMany(mappedBy = "route")
    @OrderBy("orderSequence ASC")
    private List<Order> orders = new ArrayList<>();

    @Column(nullable = false)
    private Double totalDistance = 0.0; // w km

    @Column(nullable = false)
    private Integer estimatedTimeMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteStatus status = RouteStatus.PLANNED;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
