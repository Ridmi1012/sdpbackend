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
    private String orderType; // 'as-is', 'request-similar', 'full-custom'

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

    // Fields for full-custom scenario
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

    // REMOVED: Redundant installment fields - these should be managed by Payment entity only
    // private Long installmentPlanId; - REMOVED
    // private Integer installmentTotalInstallments; - REMOVED
    // private Integer currentInstallmentNumber = 1; - REMOVED
    // private LocalDateTime nextInstallmentDueDate; - REMOVED

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

    /**
     * NEW METHOD - Gets the active payment for this order
     * Returns the payment that is not rejected or completed
     */
    public Payment getActivePayment() {
        return payments.stream()
                .filter(p -> !"rejected".equals(p.getStatus()) && !"completed".equals(p.getStatus()))
                .findFirst()
                .orElse(null);
    }

    /**
     * NEW METHOD - Checks if order has any active payments
     * Used for business logic validation
     */
    public boolean hasActivePayment() {
        return getActivePayment() != null;
    }

    /**
     * NEW METHOD - Gets the latest payment regardless of status
     * Used for displaying payment history
     */
    public Payment getLatestPayment() {
        return payments.stream()
                .max((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                .orElse(null);
    }

    /**
     * NEW METHOD - Calculates total amount paid across all confirmed payments
     * Provides accurate payment tracking at order level
     */
    public Double getTotalPaidAmount() {
        return payments.stream()
                .mapToDouble(Payment::getTotalPaid)
                .sum();
    }

    /**
     * NEW METHOD - Calculates remaining amount to be paid
     * Takes into account all confirmed payments
     */
    public Double getRemainingAmount() {
        if (totalPrice == null) return 0.0;
        return totalPrice - getTotalPaidAmount();
    }

    /**
     * NEW METHOD - Checks if order is fully paid
     * Based on actual payment confirmations
     */
    public boolean isFullyPaid() {
        if (totalPrice == null || totalPrice == 0) return true;
        return Math.abs(getRemainingAmount()) < 0.01; // Account for floating point precision
    }
}



