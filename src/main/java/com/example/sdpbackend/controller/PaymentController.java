package com.example.sdpbackend.controller;


import com.example.sdpbackend.dto.*;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.entity.Payment;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.repository.OrderRepository;
import com.example.sdpbackend.repository.PaymentRepository;
import com.example.sdpbackend.service.JWTService;
import com.example.sdpbackend.service.NotificationService;
import com.example.sdpbackend.service.PaymentService;
import com.example.sdpbackend.util.PayHereVerifier;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PayHereVerifier payHereVerifier;



    /**
     * Process a payment for an order
     */
    @PostMapping("/payment")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        logger.info("Processing payment for order ID: {}", paymentRequest.getOrderId());
        try {
            // Verify customer owns the order
            Long orderId = Long.valueOf(paymentRequest.getOrderId());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.error("Order not found with id: {}", orderId);
                        return new RuntimeException("Order not found with id: " + orderId);
                    });

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();
            logger.debug("User {} is attempting to make payment for order {}", username, orderId);

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                logger.warn("User {} attempted to make payment for order {} which they don't own", username, orderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only make payments for your own orders"));
            }

            // Check event date to ensure partial payments are only allowed for events > 24 hours away
            if (paymentRequest.getPaymentMethod().equals("bank-transfer") &&
                    paymentRequest.getAmount() < order.getTotalPrice()) {

                if (isEventWithin24Hours(order)) {
                    logger.warn("User {} attempted to make partial payment for order {} with event within 24 hours",
                            username, orderId);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Full payment is required for events within 24 hours"));
                }
            }

            OrderResponse updatedOrder = paymentService.processPayment(paymentRequest);
            logger.info("Payment processed successfully for order ID: {}", paymentRequest.getOrderId());
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to process payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to process payment: " + e.getMessage()));
        }
    }

    /**
     * Initiate PayHere payment
     * CHANGE: No longer create a payment record until successful payment
     */
    @PostMapping("/payhere/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initiatePayHerePayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        logger.info("Initiating PayHere payment for order ID: {}", paymentRequest.getOrderId());
        try {
            // Verify customer owns the order
            Long orderId = Long.valueOf(paymentRequest.getOrderId());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.error("Order not found with id: {}", orderId);
                        return new RuntimeException("Order not found with id: " + orderId);
                    });

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();
            logger.debug("User {} is attempting to initiate PayHere payment for order {}", username, orderId);

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                logger.warn("User {} attempted to initiate PayHere payment for order {} which they don't own", username, orderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only make payments for your own orders"));
            }

            // Check if this is a partial payment and event is within 24 hours
            if (paymentRequest.getAmount() < order.getTotalPrice() && isEventWithin24Hours(order)) {
                logger.warn("User {} attempted partial payment for order {} with event within 24 hours",
                        username, orderId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Full payment is required for events within 24 hours"));
            }

            Map<String, String> response = paymentService.initiatePayHerePayment(paymentRequest);
            logger.info("PayHere payment initiated successfully for order ID: {}", paymentRequest.getOrderId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to initiate PayHere payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to initiate PayHere payment: " + e.getMessage()));
        }
    }

    /**
     * Verify PayHere payment
     * CHANGE: Now we create payment record after successful verification
     */
    @PostMapping("/payhere/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> verifyPayHerePayment(@RequestBody Map<String, String> verificationRequest, HttpServletRequest request) {
        logger.info("Verifying PayHere payment for order ID: {}", verificationRequest.get("orderId"));
        try {
            String orderId = verificationRequest.get("orderId");
            String paymentId = verificationRequest.get("paymentId");

            if (orderId == null || paymentId == null) {
                logger.error("Missing orderId or paymentId in verification request");
                return ResponseEntity.badRequest().body(Map.of("message", "Order ID and Payment ID are required"));
            }

            // Verify customer owns the order
            Long orderIdLong = Long.valueOf(orderId);
            Order order = orderRepository.findById(orderIdLong)
                    .orElseThrow(() -> {
                        logger.error("Order not found with id: {}", orderId);
                        return new RuntimeException("Order not found with id: " + orderId);
                    });

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();
            logger.debug("User {} is attempting to verify PayHere payment for order {}", username, orderId);

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                logger.warn("User {} attempted to verify PayHere payment for order {} which they don't own", username, orderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only verify payments for your own orders"));
            }

            OrderResponse updatedOrder = paymentService.verifyPayHerePayment(orderId, paymentId);
            logger.info("PayHere payment verified successfully for order ID: {}, payment ID: {}", orderId, paymentId);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to verify PayHere payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to verify PayHere payment: " + e.getMessage()));
        }
    }

    /**
     * Handle PayHere notification
     * CHANGE: Create payment record here for successful payments
     */
    @PostMapping("/payhere/notify")
    public ResponseEntity<?> payHereNotification(@RequestBody Map<String, String> notification) {
        logger.info("Received PayHere notification for order ID: {}", notification.get("order_id"));
        try {
            String orderId = notification.get("order_id");
            String statusCode = notification.get("status_code");
            String paymentId = notification.get("payment_id");
            String amount = notification.get("payhere_amount");
            String currency = notification.get("payhere_currency");

            // Log the full notification for debugging
            logger.debug("PayHere notification details: {}", notification);

            // Verify the notification authenticity
            if (!payHereVerifier.verifyNotification(notification)) {
                logger.warn("PayHere notification failed verification for order ID: {}", orderId);
                // Always return 200 OK to PayHere, but log the error
                return ResponseEntity.ok().build();
            }

            // Status code 2 means success
            if ("2".equals(statusCode)) {
                logger.info("Processing successful PayHere payment for order ID: {}, PayHere payment ID: {}",
                        orderId, paymentId);

                // Find the order
                Long orderIdLong = Long.valueOf(orderId);
                Order order = orderRepository.findById(orderIdLong)
                        .orElseThrow(() -> {
                            logger.error("Order not found with id: {}", orderId);
                            return new RuntimeException("Order not found with id: " + orderId);
                        });

                // Create a new payment record
                Payment payment = new Payment();
                payment.setMethod("payhere");
                payment.setStatus("completed");
                payment.setTransactionId(paymentId);
                payment.setConfirmationDateTime(LocalDateTime.now());
                payment.setOrder(order);

                // Set payment amount from notification
                try {
                    Double paymentAmount = Double.parseDouble(amount);
                    payment.setAmount(paymentAmount);
                } catch (NumberFormatException e) {
                    // If amount can't be parsed, use the remaining amount
                    Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
                    Double totalPaid = order.getPayments().stream()
                            .filter(p -> "completed".equals(p.getStatus()))
                            .mapToDouble(Payment::getAmount)
                            .sum();
                    payment.setAmount(totalPrice - totalPaid);
                }

                // Deactivate all existing payments for this order
                for (Payment existingPayment : order.getPayments()) {
                    if (Boolean.TRUE.equals(existingPayment.getIsActive())) {
                        existingPayment.setIsActive(false);
                        paymentRepository.save(existingPayment);
                    }
                }

                // Mark this payment as active
                payment.setIsActive(true);

                // Determine if this is a partial payment
                Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
                Double totalPaid = order.getPayments().stream()
                        .filter(p -> "completed".equals(p.getStatus()))
                        .mapToDouble(Payment::getAmount)
                        .sum();
                payment.setIsPartialPayment(totalPaid + payment.getAmount() < totalPrice);
                payment.setRemainingAmount(Math.max(0, totalPrice - totalPaid - payment.getAmount()));

                // Add payment to order and save
                order.getPayments().add(payment);
                payment = paymentRepository.save(payment);
                logger.info("Created payment record for PayHere notification, payment ID: {}", payment.getId());

                // Update order payment status
                Double newTotalPaid = totalPaid + payment.getAmount();
                if (newTotalPaid >= totalPrice) {
                    order.setPaymentStatus("completed");
                    logger.info("Order payment status updated to completed for order ID: {}", order.getId());
                } else if (newTotalPaid > 0) {
                    order.setPaymentStatus("partial");
                    logger.info("Order payment status updated to partial for order ID: {}", order.getId());
                }

                // Save updated order
                orderRepository.save(order);

                // Create notification about the payment
                try {
                    notificationService.createPaymentNotification(order, payment);
                    logger.info("Payment notification created for order: {}", order.getId());
                } catch (Exception e) {
                    logger.error("Error creating payment notification: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("PayHere payment failed with status code: {} for order ID: {}", statusCode, orderId);
            }

            // Always return 200 OK to PayHere
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Log the error but still return 200 OK to PayHere
            logger.error("Error processing PayHere notification: {}", e.getMessage(), e);
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Upload payment slip
     */
    @PostMapping("/{orderId}/payment-slip-upload")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> uploadPaymentSlip(
            @PathVariable Long orderId,
            @RequestBody PaymentSlipRequest paymentSlipRequest,
            HttpServletRequest request) {
        logger.info("Uploading payment slip for order ID: {}", orderId);
        try {
            // Verify customer owns the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.error("Order not found with id: {}", orderId);
                        return new RuntimeException("Order not found with id: " + orderId);
                    });

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();
            logger.debug("User {} is attempting to upload payment slip for order {}", username, orderId);

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                logger.warn("User {} attempted to upload payment slip for order {} which they don't own", username, orderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only upload payment slips for your own orders"));
            }

            // Use explicit amount from request if provided
            Double amount = paymentSlipRequest.getAmount();
            Boolean isPartialPayment = paymentSlipRequest.getIsPartialPayment();
            String notes = paymentSlipRequest.getNotes();

            if (paymentSlipRequest.getImageUrl() == null || paymentSlipRequest.getImageUrl().isEmpty()) {
                logger.error("No image URL provided for payment slip upload for order ID: {}", orderId);
                return ResponseEntity.badRequest().body(Map.of("message", "Image URL is required"));
            }

            // Check if this is a partial payment and event is within 24 hours
            if (isPartialPayment && isEventWithin24Hours(order)) {
                logger.warn("User {} attempted partial payment upload for order {} with event within 24 hours",
                        username, orderId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Full payment is required for events within 24 hours"));
            }

            OrderResponse updatedOrder = paymentService.uploadPaymentSlip(
                    orderId,
                    paymentSlipRequest.getImageUrl(),
                    amount,
                    isPartialPayment,
                    notes
            );

            logger.info("Payment slip uploaded successfully for order ID: {}", orderId);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to upload payment slip: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to upload payment slip: " + e.getMessage()));
        }
    }

    /**
     * Verify bank transfer payment
     */
    @PostMapping("/{orderId}/payment/{paymentId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verifyManualPayment(
            @PathVariable Long orderId,
            @PathVariable Long paymentId,
            @RequestBody Map<String, Object> verificationRequest) {
        logger.info("Verifying manual payment for order ID: {}, payment ID: {}", orderId, paymentId);
        try {
            boolean isApproved = (boolean) verificationRequest.get("approved");
            String reason = verificationRequest.containsKey("reason") ?
                    (String) verificationRequest.get("reason") : null;

            OrderResponse updatedOrder = paymentService.verifyManualPayment(orderId, paymentId, isApproved, reason);
            logger.info("Manual payment verification completed for order ID: {}, payment ID: {}, approved: {}",
                    orderId, paymentId, isApproved);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to verify payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to verify payment: " + e.getMessage()));
        }
    }

    /**
     * Get payments for an order
     */
    @GetMapping("/{orderId}/payments")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrderPayments(@PathVariable Long orderId, HttpServletRequest request) {
        logger.info("Fetching payments for order ID: {}", orderId);
        try {
            // Find the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.error("Order not found with id: {}", orderId);
                        return new RuntimeException("Order not found with id: " + orderId);
                    });

            // If user is a customer, verify they can only access their own orders' payments
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();
                logger.debug("Customer user {} is attempting to access payments for order {}", username, orderId);

                // Get customer from username and verify ownership
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                    logger.warn("Customer user {} attempted to access payments for order {} which they don't own", username, orderId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access your own order payments"));
                }
            } else {
                logger.debug("Admin user is accessing payments for order {}", orderId);
            }

            List<Payment> payments = paymentRepository.findByOrderId(orderId);
            logger.info("Successfully fetched {} payments for order ID: {}", payments.size(), orderId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Failed to fetch payment details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch payment details: " + e.getMessage()));
        }
    }

    /**
     * Get payment summary for an order
     */
    @GetMapping("/{orderId}/payment-summary")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getPaymentSummary(@PathVariable Long orderId, HttpServletRequest request) {
        logger.info("Fetching payment summary for order ID: {}", orderId);
        try {
            // Find the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.error("Order not found with id: {}", orderId);
                        return new RuntimeException("Order not found with id: " + orderId);
                    });

            // If user is a customer, verify they can only access their own orders' payment summary
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();
                logger.debug("Customer user {} is attempting to access payment summary for order {}", username, orderId);

                // Get customer from username and verify ownership
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                    logger.warn("Customer user {} attempted to access payment summary for order {} which they don't own", username, orderId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access your own order payment summary"));
                }
            } else {
                logger.debug("Admin user is accessing payment summary for order {}", orderId);
            }

            PaymentSummaryDTO summary = paymentService.getPaymentSummary(orderId);
            logger.info("Successfully fetched payment summary for order ID: {}", orderId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Failed to fetch payment summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch payment summary: " + e.getMessage()));
        }
    }

    /**
     * Get available installment plans for an order
     */
    @GetMapping("/{orderId}/installment-plans")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAvailableInstallmentPlans(@PathVariable Long orderId, HttpServletRequest request) {
        logger.info("Fetching available installment plans for order ID: {}", orderId);
        try {
            // Find the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.error("Order not found with id: {}", orderId);
                        return new RuntimeException("Order not found with id: " + orderId);
                    });

            // If user is a customer, verify they can only access their own orders' installment plans
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();
                logger.debug("Customer user {} is attempting to access installment plans for order {}", username, orderId);

                // Get customer from username and verify ownership
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                    logger.warn("Customer user {} attempted to access installment plans for order {} which they don't own", username, orderId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access installment plans for your own orders"));
                }
            } else {
                logger.debug("Admin user is accessing installment plans for order {}", orderId);
            }

            List<InstallmentPlanDTO> plans = paymentService.getAvailableInstallmentPlansForOrder(orderId);
            logger.info("Successfully fetched {} installment plans for order ID: {}", plans.size(), orderId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            logger.error("Failed to fetch installment plans: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch installment plans: " + e.getMessage()));
        }
    }

    /**
     * Get next installment info for an order
     */
    @GetMapping("/{orderId}/next-installment")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getNextInstallmentInfo(@PathVariable Long orderId, HttpServletRequest request) {
        logger.info("Fetching next installment info for order ID: {}", orderId);
        try {
            // Find the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.error("Order not found with id: {}", orderId);
                        return new RuntimeException("Order not found with id: " + orderId);
                    });

            // If user is a customer, verify they can only access their own orders' installment info
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();
                logger.debug("Customer user {} is attempting to access next installment info for order {}", username, orderId);

                // Get customer from username and verify ownership
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                    logger.warn("Customer user {} attempted to access next installment info for order {} which they don't own", username, orderId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access installment info for your own orders"));
                }
            } else {
                logger.debug("Admin user is accessing next installment info for order {}", orderId);
            }

            Map<String, Object> installmentInfo = paymentService.getNextInstallmentInfo(orderId);
            logger.info("Successfully fetched next installment info for order ID: {}", orderId);
            return ResponseEntity.ok(installmentInfo);
        } catch (Exception e) {
            logger.error("Failed to fetch next installment info: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch next installment info: " + e.getMessage()));
        }
    }

    // Helper methods

    /**
     * Extract JWT token from request
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("JWT Token is missing or invalid");
    }

    /**
     * Check if event is within 24 hours
     */
    private boolean isEventWithin24Hours(Order order) {
        if (order.getEventDetails() != null && order.getEventDetails().getEventDate() != null) {
            try {
                // Parse the event date
                String eventDateStr = order.getEventDetails().getEventDate();
                LocalDateTime eventDate = LocalDateTime.parse(eventDateStr);

                // If event time is also available, combine with date
                if (order.getEventDetails().getEventTime() != null) {
                    String[] timeParts = order.getEventDetails().getEventTime().split(":");
                    int hours = Integer.parseInt(timeParts[0]);
                    int minutes = Integer.parseInt(timeParts[1]);
                    eventDate = eventDate.withHour(hours).withMinute(minutes);
                }

                // Check if event is within 24 hours
                LocalDateTime now = LocalDateTime.now();
                long hoursUntilEvent = java.time.Duration.between(now, eventDate).toHours();

                return hoursUntilEvent < 24;
            } catch (Exception e) {
                logger.error("Error parsing event date/time: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Get all pending payments that need admin verification
     */
    @GetMapping("/pending/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingVerificationPayments() {
        logger.info("Fetching pending verification payments");
        try {
            List<PaymentResponse> pendingPayments = paymentService.getPendingPaymentsAsDTO();
            logger.info("Successfully fetched {} pending verification payments", pendingPayments.size());
            return ResponseEntity.ok(pendingPayments);
        } catch (Exception e) {
            logger.error("Failed to fetch pending payments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch pending payments: " + e.getMessage()));
        }
    }

    /**
     * Get recently verified payments
     */
    @GetMapping("/payments/verified/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRecentlyVerifiedPayments() {
        logger.info("Fetching recently verified payments");
        try {
            List<PaymentResponse> verifiedPayments = paymentService.getRecentlyVerifiedPaymentsAsDTO();
            logger.info("Successfully fetched {} recently verified payments", verifiedPayments.size());
            return ResponseEntity.ok(verifiedPayments);
        } catch (Exception e) {
            logger.error("Failed to fetch recently verified payments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch recently verified payments: " + e.getMessage()));
        }
    }

    /**
     * Alternative endpoint for recently verified payments
     */
    @GetMapping("/verified/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRecentVerifiedPayments() {
        return getRecentlyVerifiedPayments();
    }

    /**
     * Get pending payments count
     */
    @GetMapping("/payments/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingPaymentsCount() {
        logger.info("Fetching pending payments count");
        try {
            // Count only non-PayHere pending payments
            long count = paymentRepository.countByStatusAndMethodNot("pending", "payhere");
            logger.info("Pending payments count: {}", count);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Failed to fetch payment count: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch payment count: " + e.getMessage()));
        }
    }


}