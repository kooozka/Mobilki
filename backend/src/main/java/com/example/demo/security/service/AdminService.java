package com.example.demo.security.service;

import com.example.demo.dispatch.model.Driver;
import com.example.demo.dispatch.repository.DriverRepository;
import com.example.demo.security.dto.CreateUserRequest;
import com.example.demo.security.dto.UpdateUserRequest;
import com.example.demo.security.dto.UserTO;
import com.example.demo.security.model.User;
import com.example.demo.security.model.UserRole;
import com.example.demo.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UserTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDTO(user);
    }

    public UserTO createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userRepository.save(user);

        if (request.getRole() == UserRole.DRIVER) {
            Driver driver = Driver.builder()
                    .user(user)
                    .licenseTypes(request.getLicenseTypes())
                    .build();
            driverRepository.save(driver);
        }

        return mapToDTO(user);
    }

    public UserTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getSuspended() != null) {
            user.setSuspended(request.getSuspended());
        }

        user = userRepository.save(user);

        if (request.getLicenseTypes() != null && user.getRole() == UserRole.DRIVER) {
            Driver driver = driverRepository.findByUser(user).orElse(null);
            if (driver != null) {
                driver.setLicenseTypes(request.getLicenseTypes());
                driverRepository.save(driver);
            }
        }

        return mapToDTO(user);
    }

    public void suspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setSuspended(true);
        userRepository.save(user);
    }

    public void unsuspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setSuspended(false);
        userRepository.save(user);
    }

    private UserTO mapToDTO(User user) {
        UserTO.UserTOBuilder builder = UserTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .suspended(user.isSuspended());

        if (user.getRole() == UserRole.DRIVER) {
            Driver driver = driverRepository.findByUser(user).orElse(null);
            if (driver != null) {
                builder.licenseTypes(driver.getLicenseTypes());
            }
        }

        return builder.build();
    }
}
