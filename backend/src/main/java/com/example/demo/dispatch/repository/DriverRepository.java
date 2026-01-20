package com.example.demo.dispatch.repository;

import com.example.demo.dispatch.model.Driver;
import com.example.demo.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUser_Email(String email);

    Optional<Driver> findByUser(User user);
}
