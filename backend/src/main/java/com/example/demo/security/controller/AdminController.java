package com.example.demo.security.controller;

import com.example.demo.security.dto.CreateUserRequest;
import com.example.demo.security.dto.UpdateUserRequest;
import com.example.demo.security.dto.UserTO;
import com.example.demo.security.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<UserTO> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/{id}")
    public UserTO getUserById(@PathVariable Long id) {
        return adminService.getUserById(id);
    }

    @PostMapping("/users")
    public UserTO createUser(@RequestBody CreateUserRequest request) {
        return adminService.createUser(request);
    }

    @PutMapping("/users/{id}")
    public UserTO updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return adminService.updateUser(id, request);
    }

    @PostMapping("/users/{id}/suspend")
    public void suspendUser(@PathVariable Long id) {
        adminService.suspendUser(id);
    }

    @PostMapping("/users/{id}/unsuspend")
    public void unsuspendUser(@PathVariable Long id) {
        adminService.unsuspendUser(id);
    }
}
