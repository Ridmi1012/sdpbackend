package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private String designId;

    @Column(nullable = false)
    private String orderType; // 'as-is', 'custom-design', etc.

    // Event details moved to a separate table EventDetails
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private EventDetails eventDetails;

    // Customer relationship with proper FK
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Order status and payment info
    @Column(nullable = false)
    private String status; // 'pending', 'viewed', 'confirmed', 'paid', etc.

    private Double basePrice;
    private Double transportationCost;
    private Double additionalRentalCost;
    private Double totalPrice;
    private String paymentStatus; // 'pending', 'partial', 'completed'
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (paymentStatus == null) {
            paymentStatus = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}



