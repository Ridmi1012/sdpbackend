package com.example.sdpbackend.util;


import com.example.sdpbackend.dto.PaymentResponse;
import com.example.sdpbackend.entity.Installment;
import com.example.sdpbackend.entity.Payment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PaymentMapper {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Convert Installment to PaymentResponse
     * This maintains compatibility with frontend
     */
    public PaymentResponse installmentToPaymentResponse(Installment installment) {
        PaymentResponse response = new PaymentResponse();

        response.setId(installment.getId());
        response.setAmount(installment.getAmount());
        response.setStatus(installment.getStatus());
        response.setMethod(installment.getPaymentMethod());
        response.setSlipUrl(installment.getPaymentSlipUrl());
        response.setNotes(installment.getNotes());
        response.setInstallmentNumber(installment.getInstallmentNumber());

        // Dates
        if (installment.getPaymentDate() != null) {
            response.setSubmittedDate(installment.getPaymentDate().format(dateFormatter));
        }

        if (installment.getConfirmationDate() != null) {
            response.setVerifiedDate(installment.getConfirmationDate().format(dateFormatter));
        }

        // Order details from parent payment
        Payment payment = installment.getPayment();
        if (payment != null && payment.getOrder() != null) {
            response.setOrderId(payment.getOrder().getId());
            response.setOrderNumber(payment.getOrder().getOrderNumber());

            if (payment.getOrder().getCustomer() != null) {
                response.setCustomerName(
                        payment.getOrder().getCustomer().getFirstName() + " " +
                                payment.getOrder().getCustomer().getLastName()
                );
            }

            if (payment.getOrder().getEventDetails() != null) {
                response.setEventDate(payment.getOrder().getEventDetails().getEventDate());
            }
        }

        // Payment type from parent
        response.setPaymentType(payment.getPaymentType());

        // Determine if this is the current active installment based on status and number
        boolean isActive = payment.getCurrentInstallmentNumber().equals(installment.getInstallmentNumber())
                && "pending".equals(installment.getStatus());
        response.setIsActive(isActive);

        // Remaining amount after this installment
        double totalPaidAfter = payment.getInstallments().stream()
                .filter(i -> i.getInstallmentNumber() <= installment.getInstallmentNumber())
                .filter(i -> "confirmed".equals(i.getStatus()))
                .mapToDouble(Installment::getAmount)
                .sum();
        response.setRemainingAmount(payment.getTotalAmount() - totalPaidAfter);

        return response;
    }

    /**
     * Convert Payment to summary format
     */
    public PaymentResponse paymentToSummaryResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();

        response.setId(payment.getId());
        response.setOrderId(payment.getOrder().getId());
        response.setOrderNumber(payment.getOrder().getOrderNumber());
        response.setPaymentType(payment.getPaymentType());
        response.setStatus(payment.getStatus());

        // Total amounts
        response.setAmount(payment.getTotalAmount());
        response.setRemainingAmount(payment.getRemainingAmount());

        // Customer info
        if (payment.getOrder().getCustomer() != null) {
            response.setCustomerName(
                    payment.getOrder().getCustomer().getFirstName() + " " +
                            payment.getOrder().getCustomer().getLastName()
            );
        }

        // Event info
        if (payment.getOrder().getEventDetails() != null) {
            response.setEventDate(payment.getOrder().getEventDetails().getEventDate());
        }

        // Dates
        if (payment.getCreatedAt() != null) {
            response.setSubmittedDate(payment.getCreatedAt().format(dateFormatter));
        }

        return response;
    }
}
