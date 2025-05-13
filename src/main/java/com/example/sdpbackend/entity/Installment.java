package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Installment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Double percentage;

    @Column(nullable = false)
    private String status; // 'pending', 'confirmed', 'rejected'

    @Column(nullable = false)
    private String paymentMethod; // 'payhere', 'bank-transfer'

    // Payment details
    private String transactionId;

    @Column(columnDefinition = "TEXT")
    private String paymentSlipUrl;

    private LocalDateTime paymentDate;
    private LocalDateTime confirmationDate;

    // Verification details
    private String verifiedBy;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        paymentDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
