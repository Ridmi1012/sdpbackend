package com.example.sdpbackend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;  // Keep only one amount field

    @Column(nullable = false)
    private String method; // 'payhere', 'bank-transfer', 'online-transfer'

    private String transactionId;
    private LocalDateTime paymentDateTime;
    private Double remainingAmount;
    private String status; // 'pending', 'completed', 'failed', 'refunded'

    @Column(columnDefinition = "TEXT")
    private String paymentSlipUrl;

    private Boolean isPartialPayment;
    private LocalDateTime confirmationDateTime;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @PrePersist
    protected void onCreate() {
        paymentDateTime = LocalDateTime.now();
    }
}
