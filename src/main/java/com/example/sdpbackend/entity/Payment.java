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

        // FIXED: Properly track installment plan ID - this was null before
        @Column(name = "installment_plan_id")
        private Long installmentPlanId;

        // FIXED: Ensure proper installment tracking
        @Column(name = "current_installment", nullable = false)
        private Integer currentInstallment = 1;

        @Column(name = "total_installments", nullable = false)
        private Integer totalInstallments = 1;

        @Column(columnDefinition = "TEXT")
        private String notes;

        @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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

        // EXISTING HELPER METHODS - No changes
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

        // EXISTING METHOD - No changes
        public Installment getCurrentInstallment() {
                return installments.stream()
                        .filter(i -> i.getInstallmentNumber().equals(currentInstallment))
                        .findFirst()
                        .orElse(null);
        }

        // EXISTING METHOD - No changes
        public Integer getCurrentInstallmentNumber() {
                return currentInstallment;
        }

        /**
         * ENHANCED METHOD - Better status update logic with proper installment tracking
         * Updates payment status based on installment confirmations
         */
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
                                // ENHANCED: Update current installment to next unconfirmed one
                                updateCurrentInstallmentToNext();
                        } else {
                                boolean hasRejected = installments.stream()
                                        .anyMatch(i -> "rejected".equals(i.getStatus()));
                                status = hasRejected ? "rejected" : "pending";
                        }
                }
        }

        /**
         * NEW METHOD - Updates current installment to the next unconfirmed installment
         * Ensures proper progression through installment payments
         */
        private void updateCurrentInstallmentToNext() {
                installments.stream()
                        .filter(i -> !"confirmed".equals(i.getStatus()))
                        .min((i1, i2) -> i1.getInstallmentNumber().compareTo(i2.getInstallmentNumber()))
                        .ifPresent(nextInstallment -> {
                                this.currentInstallment = nextInstallment.getInstallmentNumber();
                        });
        }

        /**
         * NEW METHOD - Checks if payment has a valid installment plan
         * Used for validation and business logic
         */
        public boolean hasInstallmentPlan() {
                return installmentPlanId != null && installmentPlanId > 1;
        }

        /**
         * NEW METHOD - Gets the next pending installment for payment
         * Returns null if all installments are completed or confirmed
         */
        public Installment getNextPendingInstallment() {
                return installments.stream()
                        .filter(i -> "pending".equals(i.getStatus()) || "rejected".equals(i.getStatus()))
                        .min((i1, i2) -> i1.getInstallmentNumber().compareTo(i2.getInstallmentNumber()))
                        .orElse(null);
        }

}

