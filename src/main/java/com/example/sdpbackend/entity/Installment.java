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

    @ManyToOne(fetch = FetchType.LAZY)
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

    // FIXED: Payment details - these were showing as null in database
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_slip_url", columnDefinition = "TEXT")
    private String paymentSlipUrl;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "confirmation_date")
    private LocalDateTime confirmationDate;

    // FIXED: Verification details - these were showing as null in database
    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
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
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * NEW METHOD - Confirms this installment with proper verification details
     * Sets all required fields for successful payment confirmation
     */
    public void confirmPayment(String verifierName, String transactionReference) {
        this.status = "confirmed";
        this.confirmationDate = LocalDateTime.now();
        this.verifiedBy = verifierName;
        if (transactionReference != null && !transactionReference.trim().isEmpty()) {
            this.transactionId = transactionReference;
        }
        this.rejectionReason = null; // Clear any previous rejection reason
    }

    /**
     * NEW METHOD - Rejects this installment with proper rejection details
     * Sets all required fields for payment rejection
     */
    public void rejectPayment(String verifierName, String reason) {
        this.status = "rejected";
        this.confirmationDate = LocalDateTime.now();
        this.verifiedBy = verifierName;
        this.rejectionReason = reason;
    }

    /**
     * NEW METHOD - Marks installment as pending (initial state or after rejection correction)
     * Resets verification fields to allow re-verification
     */
    public void markAsPending() {
        this.status = "pending";
        this.confirmationDate = null;
        this.verifiedBy = null;
        this.rejectionReason = null;
    }

    /**
     * NEW METHOD - Sets payment slip details for bank transfer payments
     * Used when customer uploads payment slip
     */
    public void setPaymentSlipDetails(String slipUrl, String customerNotes) {
        this.paymentSlipUrl = slipUrl;
        this.notes = customerNotes;
        this.paymentDate = LocalDateTime.now();
        if (!"pending".equals(this.status)) {
            this.status = "pending"; // Reset to pending for admin verification
        }
    }

    /**
     * NEW METHOD - Sets PayHere transaction details
     * Used for PayHere payment confirmations
     */
    public void setPayHereDetails(String payHereTransactionId) {
        this.transactionId = payHereTransactionId;
        this.paymentMethod = "payhere";
        this.paymentDate = LocalDateTime.now();
    }

    /**
     * NEW METHOD - Checks if installment is in a final state (confirmed or rejected)
     * Used for business logic validation
     */
    public boolean isFinalState() {
        return "confirmed".equals(status) || "rejected".equals(status);
    }

    /**
     * NEW METHOD - Checks if installment can be modified
     * Installments can only be modified if they're in pending state
     */
    public boolean canBeModified() {
        return "pending".equals(status);
    }

    /**
     * NEW METHOD - Gets installment display name for UI
     * Returns user-friendly installment identifier
     */
    public String getDisplayName() {
        if (payment != null && payment.getPaymentType() != null) {
            if ("full".equals(payment.getPaymentType())) {
                return "Full Payment";
            } else {
                return String.format("Installment %d of %d",
                        installmentNumber,
                        payment.getTotalInstallments());
            }
        }
        return "Installment " + installmentNumber;
    }

    /**
     * NEW METHOD - Validates installment data integrity
     * Ensures all required fields are properly set
     */
    public boolean isValid() {
        return installmentNumber != null && installmentNumber > 0 &&
                amount != null && amount > 0 &&
                percentage != null && percentage > 0 &&
                status != null && !status.trim().isEmpty() &&
                paymentMethod != null && !paymentMethod.trim().isEmpty();
    }
}
