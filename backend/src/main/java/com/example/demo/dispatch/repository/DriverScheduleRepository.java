package com.example.demo.dispatch.repository;

import com.example.demo.dispatch.model.DriverSchedule;
import com.example.demo.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverScheduleRepository extends JpaRepository<DriverSchedule, Long> {
    List<DriverSchedule> findByActiveTrue();

    Optional<DriverSchedule> findByDriverAndActiveTrue(User driver);

    List<DriverSchedule> findByDriver(User driver);

    @Query("SELECT ds FROM DriverSchedule ds WHERE :dayOfWeek MEMBER OF ds.workDays AND ds.active = true")
    List<DriverSchedule> findByWorkDaysContainingAndActiveTrue(@Param("dayOfWeek") DayOfWeek dayOfWeek);
}
