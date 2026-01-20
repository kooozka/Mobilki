package com.example.demo.security.dto;

import com.example.demo.order.model.VehicleType;
import com.example.demo.security.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String email;
    private String password;
    private UserRole role;
    // For drivers
    private Set<VehicleType> licenseTypes;
}
