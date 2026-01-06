package com.example.demo.dispatch.model;

import com.example.demo.security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "driver_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    // Pojazd usunięty - przypisywany bezpośrednio do trasy, nie do kierowcy

    @ElementCollection
    @CollectionTable(name = "driver_work_days", joinColumns = @JoinColumn(name = "schedule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "work_day")
    private Set<DayOfWeek> workDays;

    @Column(nullable = false)
    private LocalTime workStartTime;

    @Column(nullable = false)
    private LocalTime workEndTime;

    @Column(nullable = false)
    private Boolean active = true;
}
