package com.example.sdpbackend.service;

import com.example.sdpbackend.dto.OrderResponse;
import com.example.sdpbackend.dto.PaymentRequest;

import com.example.sdpbackend.dto.PaymentResponse;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.entity.Payment;
import com.example.sdpbackend.repository.OrderRepository;
import com.example.sdpbackend.repository.PaymentRepository;
import com.example.sdpbackend.util.OrderMapper;
import com.example.sdpbackend.util.PaymentMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public OrderResponse processPayment(PaymentRequest paymentRequest) {
        // Find the order
        Long orderId = Long.valueOf(paymentRequest.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Create and save payment record
        Payment payment = new Payment();
        payment.setAmount(paymentRequest.getAmount());
        payment.setMethod(paymentRequest.getPaymentMethod());
        payment.setTransactionId(paymentRequest.getTransactionId());
        payment.setStatus("completed");
        payment.setOrder(order);
        payment.setConfirmationDateTime(LocalDateTime.now());

        // Calculate remaining amount (if any)
        Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
        Double totalPaid = order.getPayments().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum() + paymentRequest.getAmount();

        payment.setRemainingAmount(Math.max(0, totalPrice - totalPaid));
        payment.setIsPartialPayment(totalPaid < totalPrice);

        // Add payment to order's payment list
        order.getPayments().add(payment);
        paymentRepository.save(payment);

        // Update order payment status
        if (totalPaid >= totalPrice) {
            order.setPaymentStatus("completed");
        } else if (totalPaid > 0) {
            order.setPaymentStatus("partial");
        }

        // Save order with updated payment status
        Order savedOrder = orderRepository.save(order);

        // Notify admin about payment
        notificationService.createPaymentNotification(savedOrder, payment);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public Map<String, String> initiatePayHerePayment(PaymentRequest paymentRequest) {
        // Find the order
        Long orderId = Long.valueOf(paymentRequest.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Create pending payment record
        Payment payment = new Payment();
        payment.setAmount(paymentRequest.getAmount());
        payment.setMethod("payhere");
        payment.setStatus("pending");
        payment.setOrder(order);

        // Calculate remaining amount
        Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
        Double totalPaid = order.getPayments().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        payment.setRemainingAmount(Math.max(0, totalPrice - totalPaid - paymentRequest.getAmount()));
        payment.setIsPartialPayment(totalPaid + paymentRequest.getAmount() < totalPrice);

        // Save payment to get ID
        Payment savedPayment = paymentRepository.save(payment);

        // Add to order's payments collection
        order.getPayments().add(savedPayment);
        orderRepository.save(order);

        // Return data needed for PayHere
        Map<String, String> response = new HashMap<>();
        response.put("paymentId", savedPayment.getId().toString());
        response.put("orderId", order.getId().toString());
        response.put("amount", paymentRequest.getAmount().toString());
        response.put("customerName", order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName());
        response.put("customerEmail", order.getCustomer().getEmail());
        response.put("customerPhone", order.getCustomer().getContact());

        return response;
    }

    @Transactional
    public OrderResponse verifyPayHerePayment(String orderId, String paymentId) {
        // Find the order
        Long orderIdLong = Long.valueOf(orderId);
        Order order = orderRepository.findById(orderIdLong)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Find the payment
        Long paymentIdLong = Long.valueOf(paymentId);
        Payment payment = paymentRepository.findById(paymentIdLong)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        // Update payment status
        payment.setStatus("completed");
        payment.setConfirmationDateTime(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update order payment status
        Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
        Double totalPaid = order.getPayments().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        if (totalPaid >= totalPrice) {
            order.setPaymentStatus("completed");
        } else if (totalPaid > 0) {
            order.setPaymentStatus("partial");
        }

        // Save order with updated payment status
        Order savedOrder = orderRepository.save(order);

        // Notify admin about payment
        notificationService.createPaymentNotification(savedOrder, payment);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse uploadPaymentSlip(Long orderId, String imageUrl, Double amount, Boolean isPartialPayment, String notes) {
        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Create payment record
        Payment payment = new Payment();

        // Set payment amount
        if (amount != null) {
            payment.setAmount(amount);
        } else {
            // If no amount specified, use the total order amount
            payment.setAmount(order.getTotalPrice() != null ? order.getTotalPrice() : 0.0);
        }

        payment.setMethod("bank-transfer");
        payment.setPaymentSlipUrl(imageUrl);
        payment.setStatus("pending"); // Needs admin verification
        payment.setOrder(order);
        payment.setIsPartialPayment(isPartialPayment != null ? isPartialPayment : false);

        // Add payment to order
        order.getPayments().add(payment);

        // Save payment
        paymentRepository.save(payment);

        // Update order status to indicate pending payment verification
        order.setPaymentStatus("pending");
        Order savedOrder = orderRepository.save(order);

        // Notify admin about new payment slip
        notificationService.createPaymentSlipNotification(savedOrder, payment);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse verifyManualPayment(Long orderId, Long paymentId, boolean isApproved, String reason) {
        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Find the payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        if (isApproved) {
            // Approve payment
            payment.setStatus("completed");
            payment.setConfirmationDateTime(LocalDateTime.now());

            // Update order payment status
            Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
            Double totalPaid = order.getPayments().stream()
                    .filter(p -> "completed".equals(p.getStatus()))
                    .mapToDouble(Payment::getAmount)
                    .sum() + payment.getAmount();

            if (totalPaid >= totalPrice) {
                order.setPaymentStatus("completed");
            } else {
                order.setPaymentStatus("partial");
            }
        } else {
            // Reject payment
            payment.setStatus("rejected");
            // Add reason if provided
            // Note: We would need to add a rejectionReason field to Payment entity for this
        }

        // Save payment and order
        paymentRepository.save(payment);
        Order savedOrder = orderRepository.save(order);

        // Notify customer about payment verification
        notificationService.createPaymentVerificationNotification(savedOrder, payment, isApproved);

        return orderMapper.toResponse(savedOrder);
    }

    public List<PaymentResponse> getPendingPaymentsAsDTO() {
        List<Payment> pendingPayments = paymentRepository.findByStatus("pending");
        return pendingPayments.stream()
                .map(paymentMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getRecentlyVerifiedPaymentsAsDTO() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Payment> verifiedPayments = paymentRepository.findByStatusAndConfirmationDateTimeAfter("completed", sevenDaysAgo);
        return verifiedPayments.stream()
                .map(paymentMapper::toDTO)
                .collect(Collectors.toList());
    }
}
