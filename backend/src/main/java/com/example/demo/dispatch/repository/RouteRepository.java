package com.example.demo.dispatch.repository;

import com.example.demo.dispatch.model.Driver;
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
    List<Route> findByDriverAndRouteDate(Driver driver, LocalDate date);

    List<Route> findByRouteDate(LocalDate date);

    List<Route> findByStatus(RouteStatus status);

    List<Route> findByDriverAndStatus(Driver driver, RouteStatus status);

    // Metody dla panelu kierowcy
    List<Route> findByDriverOrderByRouteDateDesc(Driver driver);

    List<Route> findByDriverAndStatusIn(Driver driver, List<RouteStatus> statuses);

    long countByDriver(Driver driver);

    long countByDriverAndStatus(Driver driver, RouteStatus status);

    long countByDriverAndStatusIn(Driver driver, List<RouteStatus> statuses);

    @Query("SELECT SUM(r.totalDistance) FROM Route r WHERE r.driver = :driver AND r.status = :status")
    Double sumTotalDistanceByDriverAndStatus(@Param("driver") Driver driver, @Param("status") RouteStatus status);

    // Metody do sprawdzania dostępności pojazdu/kierowcy na dany dzień
    List<Route> findByVehicleAndRouteDateAndStatusNot(Vehicle vehicle, LocalDate date, RouteStatus status);

    List<Route> findByDriverAndRouteDateAndStatusNot(Driver driver, LocalDate date, RouteStatus status);

    boolean existsByVehicleAndRouteDateAndStatusNot(Vehicle vehicle, LocalDate date, RouteStatus status);

    boolean existsByDriverAndRouteDateAndStatusNot(Driver  driver, LocalDate date, RouteStatus status);
}
