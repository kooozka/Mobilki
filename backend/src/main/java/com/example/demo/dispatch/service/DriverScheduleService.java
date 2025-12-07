package com.example.demo.dispatch.service;

import com.example.demo.dispatch.dto.DriverResponse;
import com.example.demo.dispatch.dto.DriverScheduleRequest;
import com.example.demo.dispatch.dto.DriverScheduleResponse;
import com.example.demo.dispatch.model.DriverSchedule;
import com.example.demo.dispatch.model.Vehicle;
import com.example.demo.dispatch.repository.DriverScheduleRepository;
import com.example.demo.dispatch.repository.VehicleRepository;
import com.example.demo.security.model.User;
import com.example.demo.security.model.UserRole;
import com.example.demo.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverScheduleService {

    private final DriverScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public List<DriverResponse> getAllDrivers() {
        return userRepository.findByRole(UserRole.DRIVER).stream()
                .map(this::mapToDriverResponse)
                .collect(Collectors.toList());
    }

    public List<DriverScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DriverScheduleResponse> getActiveSchedules() {
        return scheduleRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DriverScheduleResponse getScheduleById(Long id) {
        DriverSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grafiku o ID: " + id));
        return mapToResponse(schedule);
    }

    public DriverScheduleResponse getScheduleByDriver(Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kierowcy o ID: " + driverId));
        
        DriverSchedule schedule = scheduleRepository.findByDriverAndActiveTrue(driver)
                .orElseThrow(() -> new RuntimeException("Kierowca nie ma aktywnego grafiku"));
        
        return mapToResponse(schedule);
    }

    @Transactional
    public DriverScheduleResponse createSchedule(DriverScheduleRequest request) {
        User driver = userRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kierowcy o ID: " + request.getDriverId()));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new RuntimeException("Użytkownik nie jest kierowcą");
        }

        Vehicle vehicle = null;
        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono pojazdu o ID: " + request.getVehicleId()));
        }

        // Dezaktywuj poprzedni aktywny grafik
        scheduleRepository.findByDriverAndActiveTrue(driver)
                .ifPresent(existing -> {
                    existing.setActive(false);
                    scheduleRepository.save(existing);
                });

        DriverSchedule schedule = DriverSchedule.builder()
                .driver(driver)
                .assignedVehicle(vehicle)
                .workDays(request.getWorkDays())
                .workStartTime(request.getWorkStartTime())
                .workEndTime(request.getWorkEndTime())
                .active(true)
                .build();

        schedule = scheduleRepository.save(schedule);
        return mapToResponse(schedule);
    }

    @Transactional
    public DriverScheduleResponse updateSchedule(Long id, DriverScheduleRequest request) {
        DriverSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grafiku o ID: " + id));

        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono pojazdu o ID: " + request.getVehicleId()));
            schedule.setAssignedVehicle(vehicle);
        } else {
            schedule.setAssignedVehicle(null);
        }

        schedule.setWorkDays(request.getWorkDays());
        schedule.setWorkStartTime(request.getWorkStartTime());
        schedule.setWorkEndTime(request.getWorkEndTime());
        if (request.getActive() != null) {
            schedule.setActive(request.getActive());
        }

        schedule = scheduleRepository.save(schedule);
        return mapToResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new RuntimeException("Nie znaleziono grafiku o ID: " + id);
        }
        scheduleRepository.deleteById(id);
    }

    private DriverResponse mapToDriverResponse(User driver) {
        var schedule = scheduleRepository.findByDriverAndActiveTrue(driver);
        return DriverResponse.builder()
                .id(driver.getId())
                .email(driver.getEmail())
                .hasActiveSchedule(schedule.isPresent())
                .assignedVehicle(schedule.map(s -> 
                    s.getAssignedVehicle() != null ? s.getAssignedVehicle().getRegistrationNumber() : null
                ).orElse(null))
                .build();
    }

    private DriverScheduleResponse mapToResponse(DriverSchedule schedule) {
        return DriverScheduleResponse.builder()
                .id(schedule.getId())
                .driverId(schedule.getDriver().getId())
                .driverEmail(schedule.getDriver().getEmail())
                .vehicleId(schedule.getAssignedVehicle() != null ? schedule.getAssignedVehicle().getId() : null)
                .vehicleRegistration(schedule.getAssignedVehicle() != null ? schedule.getAssignedVehicle().getRegistrationNumber() : null)
                .workDays(schedule.getWorkDays())
                .workStartTime(schedule.getWorkStartTime())
                .workEndTime(schedule.getWorkEndTime())
                .active(schedule.getActive())
                .build();
    }
}
