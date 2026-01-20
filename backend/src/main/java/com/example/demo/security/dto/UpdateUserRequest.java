package com.example.demo.security.dto;

import com.example.demo.order.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String email;
    private String password;
    private Boolean suspended;
    private Set<VehicleType> licenseTypes;
}
