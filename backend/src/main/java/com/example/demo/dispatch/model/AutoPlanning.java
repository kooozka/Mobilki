package com.example.demo.dispatch.model;

import com.example.demo.dispatch.converter.AutoPlanningResultConverter;
import com.example.demo.dispatch.model.json.AutoPlanningResult;
import com.example.demo.security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "auto_planning")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoPlanning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDate planningDate;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AutoPlanningStatus status;

    @Column
    private boolean consumed = false;

    @Basic(fetch = FetchType.LAZY)
    @Convert(converter = AutoPlanningResultConverter.class)
    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private AutoPlanningResult result;
}
