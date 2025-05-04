package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private String designId;

    @Column(nullable = false)
    private String orderType; // 'as-is' | 'custom-design' | 'custom-request'

    @Column(nullable = false)
    private String status; // 'pending' | 'confirmed' | 'cancelled' | 'completed'

    @Column(nullable = false)
    private String customerId;

    // Custom Details
    private String customName;
    private String customAge;

    @Column(columnDefinition = "TEXT")
    private String venue;

    private String eventDate;
    private String eventTime;
    private String eventCategory;

    // Customer Info
    private String firstName;
    private String lastName;
    private String email;
    private String contact;
    private String relationshipToPerson;

    // Pricing
    private Double basePrice;
    private Double transportationCost;
    private Double additionalRentalCost;
    private Double totalPrice;

    // Payment
    private String paymentStatus; // 'pending' | 'partial' | 'completed'

    private String cancellationReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
