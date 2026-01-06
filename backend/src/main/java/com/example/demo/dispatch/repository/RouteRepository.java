package com.example.demo.dispatch.repository;

import com.example.demo.dispatch.model.Route;
import com.example.demo.dispatch.model.RouteStatus;
import com.example.demo.dispatch.model.Vehicle;
import com.example.demo.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByDriverAndRouteDate(User driver, LocalDate date);

    List<Route> findByRouteDate(LocalDate date);

    List<Route> findByStatus(RouteStatus status);

    List<Route> findByDriverAndStatus(User driver, RouteStatus status);

    // Metody dla panelu kierowcy
    List<Route> findByDriverOrderByRouteDateDesc(User driver);

    List<Route> findByDriverAndStatusIn(User driver, List<RouteStatus> statuses);

    long countByDriver(User driver);

    long countByDriverAndStatus(User driver, RouteStatus status);

    long countByDriverAndStatusIn(User driver, List<RouteStatus> statuses);

    @Query("SELECT SUM(r.totalDistance) FROM Route r WHERE r.driver = :driver AND r.status = :status")
    Double sumTotalDistanceByDriverAndStatus(@Param("driver") User driver, @Param("status") RouteStatus status);

    // Metody do sprawdzania dostępności pojazdu/kierowcy na dany dzień
    List<Route> findByVehicleAndRouteDateAndStatusNot(Vehicle vehicle, LocalDate date, RouteStatus status);

    List<Route> findByDriverAndRouteDateAndStatusNot(User driver, LocalDate date, RouteStatus status);

    boolean existsByVehicleAndRouteDateAndStatusNot(Vehicle vehicle, LocalDate date, RouteStatus status);

    boolean existsByDriverAndRouteDateAndStatusNot(User driver, LocalDate date, RouteStatus status);
}
