package com.example.sdpbackend.util;


import com.example.sdpbackend.dto.PaymentResponse;
import com.example.sdpbackend.entity.Payment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PaymentMapper {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public PaymentResponse toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentResponse dto = new PaymentResponse();
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setSlipUrl(payment.getPaymentSlipUrl());
        dto.setNotes(payment.getNotes());
        dto.setMethod(payment.getMethod());
        dto.setRemainingAmount(payment.getRemainingAmount());
        dto.setInstallmentNumber(payment.getInstallmentNumber());
        dto.setIsActive(payment.getIsActive());

        // Convert dates to formatted strings to avoid parsing issues
        if (payment.getPaymentDateTime() != null) {
            dto.setSubmittedDate(formatDateTime(payment.getPaymentDateTime()));
        }

        if (payment.getConfirmationDateTime() != null) {
            dto.setVerifiedDate(formatDateTime(payment.getConfirmationDateTime()));
        }

        // Set payment type based on isPartialPayment flag
        dto.setPaymentType(payment.getIsPartialPayment() != null && payment.getIsPartialPayment() ? "partial" : "full");

        // Extract order details
        if (payment.getOrder() != null) {
            dto.setOrderId(payment.getOrder().getId());
            dto.setOrderNumber(payment.getOrder().getOrderNumber());

            // Extract customer name
            if (payment.getOrder().getCustomer() != null) {
                dto.setCustomerName(
                        payment.getOrder().getCustomer().getFirstName() + " " +
                                payment.getOrder().getCustomer().getLastName()
                );
            }

            // Extract event date if available
            if (payment.getOrder().getEventDetails() != null) {
                dto.setEventDate(payment.getOrder().getEventDetails().getEventDate());
            } else {
                dto.setEventDate(""); // Set default value
            }
        }

        // Set rejection reason if applicable
        if (payment.getRejectionReason() != null) {
            dto.setRejectionReason(payment.getRejectionReason());
        }

        return dto;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(dateFormatter);
    }
}
