package com.example.sdpbackend.dto;

public class PaymentRequest {
    private String orderId;
    private Double amount;
    private String paymentMethod;
    private String transactionId;
    private Integer installmentPlanId;
    private Integer installmentNumber;
    private String notes;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getInstallmentPlanId() {
        return installmentPlanId;
    }

    public void setInstallmentPlanId(Integer installmentPlanId) {
        this.installmentPlanId = installmentPlanId;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
