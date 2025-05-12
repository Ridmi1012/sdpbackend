package com.example.sdpbackend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "installment_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer numberOfInstallments;

    @ElementCollection
    @CollectionTable(name = "installment_percentages",
            joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "percentage")
    private List<Double> percentages;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Indicates if this plan is active/available
    @Column(nullable = false)
    private Boolean isActive = true;
}
