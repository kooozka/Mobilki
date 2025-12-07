package com.example.demo.dispatch.repository;

import com.example.demo.dispatch.model.Vehicle;
import com.example.demo.order.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByAvailableTrue();
    List<Vehicle> findByType(VehicleType type);
    List<Vehicle> findByAvailableTrueAndType(VehicleType type);
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
}
