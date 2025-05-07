package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSlipRequest {
    private String imageUrl;
    private String publicId;
    private Double amount;
    private Boolean isPartialPayment;
    private String notes;
}
