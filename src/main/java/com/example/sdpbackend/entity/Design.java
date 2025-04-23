package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Designs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Design {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer designID;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "categoryID", nullable = false)
    private Category category;

    @Column(nullable = false)
    private BigDecimal basePrice;

    private String description;

    private String imageUrl;

    @Column(nullable = false)
    private Integer createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
