package com.example.demo.dispatch.service;

import com.example.demo.dispatch.dto.VehicleRequest;
import com.example.demo.dispatch.dto.VehicleResponse;
import com.example.demo.dispatch.model.Vehicle;
import com.example.demo.dispatch.repository.VehicleRepository;
import com.example.demo.order.model.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VehicleResponse> getAvailableVehicles() {
        return vehicleRepository.findByAvailableTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VehicleResponse> getVehiclesByType(VehicleType type) {
        return vehicleRepository.findByType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono pojazdu o ID: " + id));
        return mapToResponse(vehicle);
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        if (vehicleRepository.findByRegistrationNumber(request.getRegistrationNumber()).isPresent()) {
            throw new RuntimeException("Pojazd o numerze rejestracyjnym " + request.getRegistrationNumber() + " już istnieje");
        }

        // Ładowność jest predefiniowana na podstawie typu pojazdu
        Double maxWeight = request.getType().getMaxWeight();

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(request.getRegistrationNumber())
                .brand(request.getBrand())
                .model(request.getModel())
                .type(request.getType())
                .maxWeight(maxWeight)
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .notes(request.getNotes())
                .build();

        vehicle = vehicleRepository.save(vehicle);
        return mapToResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono pojazdu o ID: " + id));

        // Ładowność jest predefiniowana na podstawie typu pojazdu
        Double maxWeight = request.getType().getMaxWeight();

        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setType(request.getType());
        vehicle.setMaxWeight(maxWeight);
        vehicle.setAvailable(request.getAvailable() != null ? request.getAvailable() : vehicle.getAvailable());
        vehicle.setNotes(request.getNotes());

        vehicle = vehicleRepository.save(vehicle);
        return mapToResponse(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new RuntimeException("Nie znaleziono pojazdu o ID: " + id);
        }
        vehicleRepository.deleteById(id);
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .type(vehicle.getType())
                .maxWeight(vehicle.getMaxWeight())
                .available(vehicle.getAvailable())
                .notes(vehicle.getNotes())
                .build();
    }
}
