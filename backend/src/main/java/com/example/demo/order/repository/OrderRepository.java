package com.example.demo.order.repository;

import com.example.demo.dispatch.model.Driver;
import com.example.demo.order.model.Order;
import com.example.demo.order.model.OrderStatus;
import com.example.demo.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientOrderByCreatedAtDesc(User client);

    List<Order> findByDriverOrderByCreatedAtDesc(Driver driver);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByClientAndStatusOrderByCreatedAtDesc(User client, OrderStatus status);

    long countByStatus(OrderStatus status);

    // Nowe metody dla panelu kierowcy
    List<Order> findByDriverOrderByPickupDateDesc(Driver driver);

    List<Order> findByDriverAndStatusIn(Driver driver, List<OrderStatus> statuses);

    long countByDriver(Driver driver);

    long countByDriverAndStatus(Driver driver, OrderStatus status);

    long countByDriverAndStatusIn(Driver driver, List<OrderStatus> statuses);
}
