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
    private Long hoursUntilDeadline;
    private Boolean isDeadlinePassed;

    // List of all payments for this order
    private List<PaymentResponse> payments;

    // Active payment ID
    private Long activePaymentId;

    // Total installments property
    private Integer totalInstallments;

    // Getter and setter methods for the new fields
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

    public void setIsDeadlinePassed(Boolean isDeadlinePassed) {
        this.isDeadlinePassed = isDeadlinePassed;
    }

    public Boolean getIsDeadlinePassed() {
        return this.isDeadlinePassed;
    }

    public void setHoursUntilDeadline(Long hoursUntilDeadline) {
        this.hoursUntilDeadline = hoursUntilDeadline;
    }

    public Long getHoursUntilDeadline() {
        return this.hoursUntilDeadline;
    }

    public void setDeadlineDate(String deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public String getDeadlineDate() {
        return this.deadlineDate;
    }

    public void setNextInstallmentDueDate(String nextInstallmentDueDate) {
        this.nextInstallmentDueDate = nextInstallmentDueDate;
    }

    public String getNextInstallmentDueDate() {
        return this.nextInstallmentDueDate;
    }

    public void setNextInstallmentAmount(Double nextInstallmentAmount) {
        this.nextInstallmentAmount = nextInstallmentAmount;
    }

    public Double getNextInstallmentAmount() {
        return this.nextInstallmentAmount;
    }

    public void setCurrentInstallment(Integer currentInstallment) {
        this.currentInstallment = currentInstallment;
    }

    public Integer getCurrentInstallment() {
        return this.currentInstallment;
    }

    public void setInstallmentPlan(InstallmentPlan installmentPlan) {
        this.installmentPlan = installmentPlan;
    }

    public InstallmentPlan getInstallmentPlan() {
        return this.installmentPlan;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentStatus() {
        return this.paymentStatus;
    }

    public void setIsFullyPaid(Boolean isFullyPaid) {
        this.isFullyPaid = isFullyPaid;
    }

    public Boolean getIsFullyPaid() {
        return this.isFullyPaid;
    }

    public void setRemainingAmount(Double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public Double getRemainingAmount() {
        return this.remainingAmount;
    }

    public void setTotalPaid(Double totalPaid) {
        this.totalPaid = totalPaid;
    }

    public Double getTotalPaid() {
        return this.totalPaid;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getTotalAmount() {
        return this.totalAmount;
    }

    public void setPayments(List<PaymentResponse> payments) {
        this.payments = payments;
    }

    public List<PaymentResponse> getPayments() {
        return this.payments;
    }
}
