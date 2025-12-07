package com.example.demo.dispatch.repository;

import com.example.demo.dispatch.model.DriverSchedule;
import com.example.demo.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverScheduleRepository extends JpaRepository<DriverSchedule, Long> {
    List<DriverSchedule> findByActiveTrue();
    Optional<DriverSchedule> findByDriverAndActiveTrue(User driver);
    List<DriverSchedule> findByDriver(User driver);
}
