package com.example.sdpbackend.controller;


import com.example.sdpbackend.dto.*;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.Installment;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.entity.Payment;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.repository.InstallmentRepository;
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
import java.util.stream.Collectors;

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

    @Autowired
    private InstallmentRepository installmentRepository;


    /**
     * Process a payment for an order (bank transfer only)
     */
    @PostMapping("/payment")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        logger.info("Processing payment for order ID: {}", paymentRequest.getOrderId());
        try {
            // Verify customer owns the order
            Long orderId = Long.valueOf(paymentRequest.getOrderId());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if ("bank-transfer".equals(paymentRequest.getPaymentMethod()) &&
                    paymentRequest.getTransactionId() == null) {
                // Generate a transaction ID if not provided
                paymentRequest.setTransactionId("BT-" + System.currentTimeMillis());
            }

            OrderResponse updatedOrder = paymentService.processPayment(paymentRequest);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to process payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to process payment: " + e.getMessage()));
        }
    }


    /**
     * Initiate PayHere payment (no DB records created)
     */
    @PostMapping("/payhere/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initiatePayHerePayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        logger.info("Initiating PayHere payment for order ID: {}", paymentRequest.getOrderId());
        try {
            // Verify ownership
            Long orderId = Long.valueOf(paymentRequest.getOrderId());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only make payments for your own orders"));
            }

            // Ensure PayHere method
            paymentRequest.setPaymentMethod("payhere");

            // Process payment (this will only return PayHere params, no DB records)
            OrderResponse response = paymentService.processPayment(paymentRequest);

            // Return PayHere parameters directly
            return ResponseEntity.ok(response.getPayHereParams());
        } catch (Exception e) {
            logger.error("Failed to initiate PayHere payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to initiate PayHere payment: " + e.getMessage()));
        }
    }

    /**
     * Verify PayHere payment (creates DB records on success)
     */
    @PostMapping("/payhere/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> verifyPayHerePayment(@RequestBody Map<String, String> verificationRequest, HttpServletRequest request) {
        logger.info("Verifying PayHere payment for order ID: {}", verificationRequest.get("orderId"));
        try {
            String orderId = verificationRequest.get("orderId");
            String paymentId = verificationRequest.get("paymentId");

            OrderResponse updatedOrder = paymentService.verifyPayHerePayment(orderId, paymentId);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to verify PayHere payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to verify PayHere payment: " + e.getMessage()));
        }
    }

    /**
     * Handle PayHere notification (server-to-server)
     */
    @PostMapping("/payhere/notify")
    public ResponseEntity<?> payHereNotification(@RequestBody Map<String, String> notification) {
        logger.info("Received PayHere notification for order ID: {}", notification.get("order_id"));
        try {
            String orderId = notification.get("order_id");
            String statusCode = notification.get("status_code");
            String paymentId = notification.get("payment_id");

            // Verify notification authenticity
            if (!payHereVerifier.verifyNotification(notification)) {
                logger.warn("PayHere notification failed verification");
                return ResponseEntity.ok().build();
            }

            // Status code 2 means success
            if ("2".equals(statusCode)) {
                // Create payment records and confirm payment
                paymentService.verifyPayHerePayment(orderId, paymentId);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing PayHere notification: {}", e.getMessage(), e);
            return ResponseEntity.ok().build();
        }
    }


    /**
     * Upload payment slip (bank transfer)
     */
    @PostMapping("/{orderId}/payment-slip-upload")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> uploadPaymentSlip(
            @PathVariable Long orderId,
            @RequestBody PaymentSlipRequest paymentSlipRequest,
            HttpServletRequest request) {
        logger.info("Uploading payment slip for order ID: {}", orderId);
        try {
            // Verify ownership
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only upload payment slips for your own orders"));
            }

            OrderResponse updatedOrder = paymentService.uploadPaymentSlip(
                    orderId,
                    paymentSlipRequest.getImageUrl(),
                    paymentSlipRequest.getAmount(),
                    paymentSlipRequest.getIsPartialPayment(),
                    paymentSlipRequest.getNotes()
            );
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to upload payment slip: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to upload payment slip: " + e.getMessage()));
        }
    }

    /**
     * Verify bank transfer payment (admin only)
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
            // Get all installments for this order
            List<Installment> installments = installmentRepository.findByOrderId(orderId);

            // Convert to PaymentResponse format for frontend compatibility
            List<PaymentResponse> responses = installments.stream()
                    .map(installment -> paymentService.getPaymentMapper().installmentToPaymentResponse(installment))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
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
            PaymentSummaryDTO summary = paymentService.getPaymentSummary(orderId);
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
            List<InstallmentPlanDTO> plans = paymentService.getAvailableInstallmentPlansForOrder(orderId);
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
            Map<String, Object> installmentInfo = paymentService.getNextInstallmentInfo(orderId);
            logger.info("Successfully fetched next installment info for order ID: {}", orderId);
            return ResponseEntity.ok(installmentInfo);
        } catch (Exception e) {
            logger.error("Failed to fetch next installment info: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch next installment info: " + e.getMessage()));
        }
    }

    // Helper method
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
     * Get all pending payments that need verification
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

     /* Alternative endpoint for recently verified payments
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
            long count = installmentRepository.countPendingBankTransfers();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Failed to fetch payment count: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch payment count: " + e.getMessage()));
        }
    }
}