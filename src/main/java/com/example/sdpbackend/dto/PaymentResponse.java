package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String eventDate;
    private Double amount;
    private String paymentType; // 'full' or 'partial'
    private String method; // 'payhere' or 'bank-transfer'
    private String slipUrl; // Same as paymentSlipUrl
    private String notes;
    private String submittedDate; // Using String format
    private String status; // 'pending', 'completed', 'rejected'
    private String verifiedDate;
    private String verifiedBy;
    private String rejectionReason;
    private Integer installmentNumber;
    private Double remainingAmount;
    private Boolean isActive; // Added to track current payment
}
