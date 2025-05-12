package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSlipRequest {
    private String imageUrl;
    private Double amount;
    private Boolean isPartialPayment;
    private Integer installmentPlanId;
    private Integer installmentNumber;
    private String notes;
}
