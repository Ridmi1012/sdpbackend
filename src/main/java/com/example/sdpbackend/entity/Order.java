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
    private String orderType; // 'as-is', 'request-similar', 'full-custom' // CHANGED - added full-custom

    @Column(nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Long designId;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private EventDetails eventDetails;

    // Fields for request-similar and full-custom
    private String themeColor;
    private String conceptCustomization;

    // NEW FIELDS for full-custom scenario
    @ElementCollection
    @CollectionTable(name = "order_inspiration_photos", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "photo_url")
    private List<String> inspirationPhotos = new ArrayList<>(); // Max 3 photos

    @Column(length = 1000)
    private String specialNote; // Special instructions for full-custom orders

    // Order items for request-similar and full-custom orders
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Double basePrice;
    private Double transportationCost;
    private Double additionalRentalCost;
    private Double totalPrice;

    private String paymentStatus;

    private Long installmentPlanId;
    private Integer installmentTotalInstallments;
    private Integer currentInstallmentNumber = 1;
    private LocalDateTime nextInstallmentDueDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();

    private String cancellationReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "pending";
        }
        if (paymentStatus == null) {
            paymentStatus = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}



