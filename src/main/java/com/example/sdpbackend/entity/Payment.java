package com.example.sdpbackend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne
        @JoinColumn(name = "order_id", nullable = false)
        private Order order;

        @Column(nullable = false)
        private Double totalAmount;

        @Column(nullable = false)
        private Double amount;

        @Column(nullable = false)
        private String paymentMethod; // 'payhere', 'bank-transfer'

        @Column(nullable = false)
        private String paymentType; // 'full', 'installment'

        @Column(nullable = false)
        private String status; // 'pending', 'partial', 'completed', 'rejected'

        // Installment tracking
        private Long installmentPlanId;
        private Integer currentInstallment = 1;
        private Integer totalInstallments = 1;

        @Column(columnDefinition = "TEXT")
        private String notes;

        @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Installment> installments = new ArrayList<>();

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

        // Helper methods
        public boolean isFullyPaid() {
                if ("full".equals(paymentType)) {
                        return "completed".equals(status);
                }
                return installments.stream()
                        .allMatch(i -> "confirmed".equals(i.getStatus()));
        }

        public Double getTotalPaid() {
                return installments.stream()
                        .filter(i -> "confirmed".equals(i.getStatus()))
                        .mapToDouble(Installment::getAmount)
                        .sum();
        }

        public Double getRemainingAmount() {
                return totalAmount - getTotalPaid();
        }

        // Get the current installment object
        public Installment getCurrentInstallment() {
                return installments.stream()
                        .filter(i -> i.getInstallmentNumber().equals(currentInstallment))
                        .findFirst()
                        .orElse(null);
        }

        // Get the current installment number
        public Integer getCurrentInstallmentNumber() {
                return currentInstallment;
        }

        public void updateStatus() {
                if ("full".equals(paymentType)) {
                        // For full payment, status depends on single payment confirmation
                        Installment single = installments.isEmpty() ? null : installments.get(0);
                        if (single != null) {
                                status = "confirmed".equals(single.getStatus()) ? "completed" :
                                        "rejected".equals(single.getStatus()) ? "rejected" : "pending";
                        }
                } else {
                        // For installment payment, check all installments
                        long confirmedCount = installments.stream()
                                .filter(i -> "confirmed".equals(i.getStatus()))
                                .count();

                        if (confirmedCount == totalInstallments) {
                                status = "completed";
                        } else if (confirmedCount > 0) {
                                status = "partial";
                        } else {
                                boolean hasRejected = installments.stream()
                                        .anyMatch(i -> "rejected".equals(i.getStatus()));
                                status = hasRejected ? "rejected" : "pending";
                        }
                }
        }

}

