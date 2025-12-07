package com.example.demo.dispatch.repository;

import com.example.demo.dispatch.model.Route;
import com.example.demo.dispatch.model.RouteStatus;
import com.example.demo.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByDriverAndRouteDate(User driver, LocalDate date);
    List<Route> findByRouteDate(LocalDate date);
    List<Route> findByStatus(RouteStatus status);
    List<Route> findByDriverAndStatus(User driver, RouteStatus status);
}
