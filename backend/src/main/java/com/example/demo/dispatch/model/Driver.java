package com.example.demo.dispatch.model;

import com.example.demo.order.model.VehicleType;
import com.example.demo.security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "driver")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // lista uprawnień - typy pojazdów, które kierowca może prowadzić
    @ElementCollection
    @CollectionTable(name = "driver_license_types", joinColumns = @JoinColumn(name = "driver_id"))
    @Column(name = "license_type")
    @Enumerated(EnumType.STRING)
    private Set<VehicleType> licenseTypes = new HashSet<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<DriverSchedule> schedules = Collections.emptySet();
}

