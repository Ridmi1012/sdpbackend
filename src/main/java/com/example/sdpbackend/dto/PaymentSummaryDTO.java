package com.example.sdpbackend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryDTO {
    private Double totalAmount;
    private Double totalPaid;
    private Double remainingAmount;
    private Boolean isFullyPaid;
    private String paymentStatus; // 'pending', 'partial', 'completed'

    // Installment plan details
    private Map<String, Object> installmentPlan;
    private Integer currentInstallment;
    private Double nextInstallmentAmount;
    private String nextInstallmentDueDate;

    // Payment deadline
    private String deadlineDate;

    // List of all payments for this order
    private List<PaymentResponse> payments;

    // Add missing field for active payment ID
    private Long activePaymentId;

    public void setActivePaymentId(Long id) {
        this.activePaymentId = id;
    }

    public Long getActivePaymentId() {
        return this.activePaymentId;
    }
}
