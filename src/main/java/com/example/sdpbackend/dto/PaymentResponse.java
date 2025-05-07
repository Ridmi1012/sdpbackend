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
    private String eventDate; // Using String instead of LocalDateTime to avoid parsing issues
    private Double amount;
    private String paymentType; // 'full' or 'partial'
    private String slipUrl; // Same as paymentSlipUrl
    private String slipThumbnail; // Optional thumbnail version
    private String notes;
    private String submittedDate; // Using String format
    private String status; // 'pending', 'verified', 'rejected'
    private String verifiedDate;
    private String verifiedBy;
    private String rejectionReason;
}
