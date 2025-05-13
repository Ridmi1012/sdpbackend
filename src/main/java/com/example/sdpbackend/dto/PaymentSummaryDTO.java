package com.example.sdpbackend.dto;


import com.example.sdpbackend.entity.InstallmentPlan;
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

    // Installment plan details (changed from Map to proper type)
    private InstallmentPlan installmentPlan;
    private Integer currentInstallment;
    private Double nextInstallmentAmount;
    private String nextInstallmentDueDate;

    // Payment deadline
    private String deadlineDate;

    // List of all payments for this order
    private List<PaymentResponse> payments;

    // Add missing field for active payment ID
    private Long activePaymentId;

    // Add missing totalInstallments property
    private Integer totalInstallments;

    public void setActivePaymentId(Long id) {
        this.activePaymentId = id;
    }

    public Long getActivePaymentId() {
        return this.activePaymentId;
    }

    public void setTotalInstallments(Integer totalInstallments) {
        this.totalInstallments = totalInstallments;
    }

    public Integer getTotalInstallments() {
        return this.totalInstallments;
    }
}
