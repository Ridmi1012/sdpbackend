package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    // Add relationship with DesignItem to fix "Cannot resolve method 'getItems' in 'Design'" error
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DesignItem> items = new ArrayList<>();

    // Add additional images support
    @ElementCollection
    @CollectionTable(name = "design_additional_images", joinColumns = @JoinColumn(name = "design_id"))
    @Column(name = "image_url")
    private List<String> additionalImages = new ArrayList<>();
}
