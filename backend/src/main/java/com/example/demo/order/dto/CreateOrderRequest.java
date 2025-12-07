package com.example.demo.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Nazwa zlecenia jest wymagana")
    private String title;
    
    @NotBlank(message = "Lokalizacja odbioru jest wymagana")
    private String pickupLocation;
    
    @NotBlank(message = "Adres odbioru jest wymagany")
    private String pickupAddress;
    
    @NotNull(message = "Data odbioru jest wymagana")
    private LocalDate pickupDate;
    
    @NotBlank(message = "Lokalizacja dostawy jest wymagana")
    private String deliveryLocation;
    
    @NotBlank(message = "Adres dostawy jest wymagany")
    private String deliveryAddress;
    
    @NotNull(message = "Termin dostawy jest wymagany")
    private LocalDate deliveryDeadline;
    
    // vehicleType jest opcjonalne - będzie automatycznie przypisane na podstawie wagi
    private String vehicleType;
    
    @NotNull(message = "Waga ładunku jest wymagana")
    @Positive(message = "Waga ładunku musi być większa od 0")
    private Double cargoWeight;
    
    private String description;
}
