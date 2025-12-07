package com.example.demo.order.repository;

import com.example.demo.order.model.Order;
import com.example.demo.order.model.OrderStatus;
import com.example.demo.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientOrderByCreatedAtDesc(User client);
    List<Order> findByDriverOrderByCreatedAtDesc(User driver);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByClientAndStatusOrderByCreatedAtDesc(User client, OrderStatus status);
    long countByStatus(OrderStatus status);
}
