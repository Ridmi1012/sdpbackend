package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id; // This will be installment ID
    private Long orderId;
    private String orderNumber;
    private String customerName; // NEW: Customer name for display
    private String eventDate; // NEW: Event date for display
    private Double amount;
    private String paymentType; // 'full' or 'installment'
    private String method; // 'payhere' or 'bank-transfer'
    private String slipUrl;
    private String notes;
    private String status; // 'pending', 'completed', 'rejected', 'partial'
    private String submittedDate;
    private String verifiedDate;
    private String verifiedBy;
    private String rejectionReason;
    private Integer installmentNumber;
    private Double remainingAmount;
    private Boolean isActive;
}
