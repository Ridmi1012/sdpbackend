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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    private PayHereVerifier payHereVerifier;

    @Autowired
    private InstallmentRepository installmentRepository;

    /**
     * Process a payment for an order (bank transfer only)
     * This creates the payment record for bank transfer
     */
    @PostMapping("/payment")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        logger.info("Processing payment for order ID: {} with installment plan: {}",
                paymentRequest.getOrderId(), paymentRequest.getInstallmentPlanId());
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
            if (customerOpt.isEmpty() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only make payments for your own orders"));
            }

            // Only allow bank transfer through this endpoint
            if (!"bank-transfer".equals(paymentRequest.getPaymentMethod())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "This endpoint is only for bank transfer payments. Use PayHere endpoints for PayHere payments."));
            }

            // Generate a transaction ID if not provided
            if (paymentRequest.getTransactionId() == null) {
                paymentRequest.setTransactionId("BT-" + System.currentTimeMillis());
            }

            // Validate installment plan selection
            if (paymentRequest.getInstallmentPlanId() != null && paymentRequest.getInstallmentPlanId() > 1) {
                validateInstallmentPlanSelection(order, paymentRequest.getInstallmentPlanId());
            }

            OrderResponse updatedOrder = paymentService.processPayment(paymentRequest);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to process payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to process payment: " + e.getMessage()));
        }
    }

    /**
     * Validate installment plan selection based on event timing
     */
    private void validateInstallmentPlanSelection(Order order, Integer installmentPlanId) {
        // Get available plans for this order
        List<InstallmentPlanDTO> availablePlans = paymentService.getAvailableInstallmentPlansForOrder(order.getId());

        boolean isValidPlan = availablePlans.stream()
                .anyMatch(plan -> plan.getId().equals(installmentPlanId));

        if (!isValidPlan) {
            throw new RuntimeException("Selected installment plan is not available for this order due to timing constraints");
        }
    }

    /**
     * Initiate PayHere payment (NO DB records created until verification)
     */
    @PostMapping("/payhere/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initiatePayHerePayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        logger.info("Initiating PayHere payment for order ID: {} with installment plan: {} installment: {}",
                paymentRequest.getOrderId(), paymentRequest.getInstallmentPlanId(), paymentRequest.getInstallmentNumber());
        try {
            // Verify ownership
            Long orderId = Long.valueOf(paymentRequest.getOrderId());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (customerOpt.isEmpty() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only make payments for your own orders"));
            }

            // Validate installment plan selection for PayHere
            if (paymentRequest.getInstallmentPlanId() != null && paymentRequest.getInstallmentPlanId() > 1) {
                validateInstallmentPlanSelection(order, paymentRequest.getInstallmentPlanId());
            }

            // Ensure PayHere method
            paymentRequest.setPaymentMethod("payhere");

            // This only generates PayHere params, no DB records created
            OrderResponse response = paymentService.processPayment(paymentRequest);

            // Return PayHere parameters directly
            return ResponseEntity.ok(response.getPayHereParams());
        } catch (Exception e) {
            logger.error("Failed to initiate PayHere payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to initiate PayHere payment: " + e.getMessage()));
        }
    }

    /**
     * Verify PayHere payment (creates DB records ONLY on success)
     */
    @PostMapping("/payhere/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> verifyPayHerePayment(@RequestBody Map<String, String> verificationRequest, HttpServletRequest request) {
        logger.info("Verifying PayHere payment for order ID: {}", verificationRequest.get("orderId"));
        try {
            String orderId = verificationRequest.get("orderId");
            String paymentId = verificationRequest.get("paymentId");

            if (orderId == null || paymentId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Order ID and Payment ID are required"));
            }

            // This will create Payment records ONLY if PayHere payment was successful
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
                logger.warn("PayHere notification failed verification for order: {}", orderId);
                return ResponseEntity.ok().build();
            }

            // Status code 2 means success
            if ("2".equals(statusCode)) {
                // Create payment records ONLY on successful PayHere payment
                paymentService.verifyPayHerePayment(orderId, paymentId);
                logger.info("PayHere payment processed successfully for order: {}", orderId);
            } else {
                logger.warn("PayHere payment failed with status code: {} for order: {}", statusCode, orderId);
                // Do NOT create any records for failed payments
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing PayHere notification: {}", e.getMessage(), e);
            return ResponseEntity.ok().build();
        }
    }

    /**
     * FIXED: Upload payment slip for bank transfer
     */
    @PostMapping("/{orderId}/payment-slip")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> uploadPaymentSlip(@PathVariable Long orderId,
                                               @RequestBody PaymentSlipRequest request,
                                               HttpServletRequest httpRequest) {
        logger.info("Uploading payment slip for order: {}", orderId);
        try {
            // Verify ownership
            String token = extractTokenFromRequest(httpRequest);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (customerOpt.isEmpty() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only upload payment slips for your own orders"));
            }

            // FIXED: Use the corrected upload method
            OrderResponse updatedOrder = paymentService.uploadPaymentSlip(
                    orderId,
                    request.getImageUrl(),
                    request.getAmount(),
                    request.getIsPartialPayment(),
                    request.getNotes()
            );

            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to upload payment slip: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to upload payment slip: " + e.getMessage()));
        }
    }

    /**
     * FIXED METHOD - Verify bank transfer payment using installment ID instead of payment ID
     * Updated to work directly with installment records which is how the system actually works
     */
    @PostMapping("/{orderId}/payment/{installmentId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> verifyManualPayment(
            @PathVariable Long orderId,
            @PathVariable Long installmentId, // CHANGED: This is actually an installment ID, not payment ID
            @RequestBody Map<String, Object> verificationRequest) {

        logger.info("Verifying manual payment for order ID: {}, installment ID: {}", orderId, installmentId);

        try {
            // ENHANCED: Validate input parameters first
            if (orderId == null || installmentId == null) {
                logger.error("Invalid parameters: orderId={}, installmentId={}", orderId, installmentId);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Order ID and Installment ID are required"));
            }

            // ENHANCED: Validate verification request body
            if (verificationRequest == null || !verificationRequest.containsKey("approved")) {
                logger.error("Invalid verification request body");
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Approval status is required"));
            }

            boolean isApproved = (boolean) verificationRequest.get("approved");
            String reason = verificationRequest.containsKey("reason") ?
                    (String) verificationRequest.get("reason") : null;

            // ENHANCED: Validate order exists first
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                logger.error("Order not found with ID: {}", orderId);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Order not found with ID: " + orderId));
            }

            // FIXED: Find installment directly instead of payment
            Installment installment = installmentRepository.findById(installmentId).orElse(null);
            if (installment == null) {
                logger.error("Installment not found with ID: {}", installmentId);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Installment not found with ID: " + installmentId));
            }

            // ENHANCED: Validate installment belongs to the specified order
            if (!installment.getPayment().getOrder().getId().equals(orderId)) {
                logger.error("Installment {} does not belong to order {}", installmentId, orderId);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Installment does not belong to the specified order"));
            }

            // ENHANCED: Validate rejection reason if payment is being rejected
            if (!isApproved && (reason == null || reason.trim().isEmpty())) {
                logger.error("Rejection reason is required when rejecting payment");
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Rejection reason is required when rejecting payment"));
            }

            // FIXED: Process the verification using installment ID
            OrderResponse updatedOrder = paymentService.verifyManualPaymentByInstallment(orderId, installmentId, isApproved, reason);

            if (isApproved) {
                logger.info("Installment approved successfully for order: {} by admin", orderId);
            } else {
                logger.info("Installment rejected for order: {} - Reason: {} by admin", orderId, reason);
            }

            return ResponseEntity.ok(updatedOrder);

        } catch (RuntimeException e) {
            // ENHANCED: Better error categorization and logging
            logger.error("Business logic error verifying installment {}: {}", installmentId, e.getMessage());

            String errorMessage = e.getMessage();
            if (errorMessage.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", errorMessage));
            } else if (errorMessage.contains("invalid") || errorMessage.contains("Invalid")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", errorMessage));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Failed to verify installment: " + errorMessage));
            }

        } catch (Exception e) {
            // ENHANCED: Handle unexpected errors more gracefully
            logger.error("Unexpected error verifying installment {}: {}", installmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred while verifying the payment. Please try again."));
        }
    }

    /**
     * ENHANCED METHOD - Get payments for an order with better error handling
     * FIXED: More robust payment retrieval with proper error handling
     */
    @GetMapping("/{orderId}/payments")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrderPayments(@PathVariable Long orderId, HttpServletRequest request) {
        logger.info("Fetching payments for order ID: {}", orderId);

        try {
            // ENHANCED: Validate order exists first
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                logger.error("Order not found with ID: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Order not found with ID: " + orderId));
            }

            // ENHANCED: Check user authorization for this specific order
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (customerOpt.isEmpty() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                    logger.warn("Customer {} attempted to access payments for order {} without authorization", username, orderId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access payments for your own orders"));
                }
            }

            // Get all installments for this order, sorted by installment number
            List<Installment> installments = installmentRepository.findByOrderId(orderId);

            // Sort installments by installment number for better display
            installments.sort((i1, i2) -> i1.getInstallmentNumber().compareTo(i2.getInstallmentNumber()));

            // ENHANCED: Better error handling for empty results
            if (installments.isEmpty()) {
                logger.info("No payment records found for order {}", orderId);
                return ResponseEntity.ok(List.of()); // Return empty list instead of error
            }

            // Convert to PaymentResponse format for frontend compatibility
            List<PaymentResponse> responses = installments.stream()
                    .map(installment -> paymentService.getPaymentMapper().installmentToPaymentResponse(installment))
                    .collect(Collectors.toList());

            logger.info("Successfully fetched {} payment records for order {}", responses.size(), orderId);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to fetch payment details for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch payment details: " + e.getMessage()));
        }
    }

    /**
     * ENHANCED METHOD - Get payment summary with better validation
     * FIXED: More robust summary generation with proper error handling
     */
    @GetMapping("/{orderId}/payment-summary")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getPaymentSummary(@PathVariable Long orderId, HttpServletRequest request) {
        logger.info("Fetching payment summary for order ID: {}", orderId);

        try {
            // ENHANCED: Validate order exists first
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                logger.error("Order not found with ID: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Order not found with ID: " + orderId));
            }

            // ENHANCED: Check user authorization
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (customerOpt.isEmpty() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                    logger.warn("Customer {} attempted to access payment summary for order {} without authorization", username, orderId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access payment summaries for your own orders"));
                }
            }

            PaymentSummaryDTO summary = paymentService.getPaymentSummary(orderId);

            // ENHANCED: Log installment progress if available with more details
            if (summary.getInstallmentPlan() != null) {
                logger.info("Order {} has installment plan: {} ({} installments, current: {}, total paid: {})",
                        orderId,
                        summary.getInstallmentPlan().getName(),
                        summary.getTotalInstallments(),
                        summary.getCurrentInstallment(),
                        summary.getTotalPaid());
            } else {
                logger.info("Order {} payment summary - Total: {}, Paid: {}, Remaining: {}, Status: {}",
                        orderId,
                        summary.getTotalAmount(),
                        summary.getTotalPaid(),
                        summary.getRemainingAmount(),
                        summary.getPaymentStatus());
            }

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            logger.error("Failed to fetch payment summary for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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

            // Log the number of available plans and their details
            logger.info("Found {} available installment plans for order {}", plans.size(), orderId);
            for (InstallmentPlanDTO plan : plans) {
                logger.debug("Available plan: {} ({} installments) - {}",
                        plan.getName(), plan.getNumberOfInstallments(), plan.getTimeRequirement());
            }

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

            // Log detailed installment information
            if (installmentInfo.containsKey("hasInstallmentPlan") &&
                    (Boolean) installmentInfo.get("hasInstallmentPlan")) {
                logger.info("Order {} installment info: {}/{} installments, next amount: {}",
                        orderId,
                        installmentInfo.get("currentInstallmentNumber"),
                        installmentInfo.get("totalInstallments"),
                        installmentInfo.get("nextInstallmentAmount"));
            }

            return ResponseEntity.ok(installmentInfo);
        } catch (Exception e) {
            logger.error("Failed to fetch next installment info: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch next installment info: " + e.getMessage()));
        }
    }

    /**
     * Get installment dropdown options for payment dialog
     */
    @GetMapping("/{orderId}/installment-options")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getInstallmentOptions(@PathVariable Long orderId, HttpServletRequest request) {
        logger.info("Fetching installment options for order ID: {}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Check for active payment
            Payment activePayment = paymentService.getActivePaymentForOrder(order);

            if (activePayment == null || !"installment".equals(activePayment.getPaymentType())) {
                // No installment plan - return basic info
                Map<String, Object> noInstallmentResponse = new HashMap<>();
                noInstallmentResponse.put("hasInstallmentPlan", false);
                noInstallmentResponse.put("message", "No active installment plan found for this order");
                return ResponseEntity.ok(noInstallmentResponse);
            }

            // Get all installments with their detailed status
            List<Map<String, Object>> installmentOptions = activePayment.getInstallments().stream()
                    .sorted((i1, i2) -> i1.getInstallmentNumber().compareTo(i2.getInstallmentNumber()))
                    .map(installment -> {
                        boolean isCurrent = installment.getInstallmentNumber().equals(activePayment.getCurrentInstallmentNumber());
                        boolean isCompleted = "confirmed".equals(installment.getStatus());
                        boolean isClickable = isCurrent && !"confirmed".equals(installment.getStatus());

                        Map<String, Object> option = new HashMap<>();
                        option.put("number", installment.getInstallmentNumber());
                        option.put("amount", installment.getAmount());
                        option.put("percentage", installment.getPercentage());
                        option.put("status", installment.getStatus());
                        option.put("isClickable", isClickable);
                        option.put("isCurrent", isCurrent);
                        option.put("isCompleted", isCompleted);
                        option.put("transactionId", installment.getTransactionId() != null ? installment.getTransactionId() : "");
                        option.put("paymentMethod", installment.getPaymentMethod() != null ? installment.getPaymentMethod() : "");
                        option.put("description", String.format("Installment %d of %d (%.1f%%)",
                                installment.getInstallmentNumber(),
                                activePayment.getTotalInstallments(),
                                installment.getPercentage()));
                        return option;
                    })
                    .collect(Collectors.toList());

            // Calculate progress information
            long completedInstallments = activePayment.getInstallments().stream()
                    .filter(i -> "confirmed".equals(i.getStatus()))
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("hasInstallmentPlan", true);
            response.put("currentInstallment", activePayment.getCurrentInstallmentNumber());
            response.put("totalInstallments", activePayment.getTotalInstallments());
            response.put("completedInstallments", completedInstallments);
            response.put("remainingInstallments", activePayment.getTotalInstallments() - completedInstallments);
            response.put("progressPercentage", (completedInstallments * 100.0) / activePayment.getTotalInstallments());
            response.put("totalPaid", activePayment.getTotalPaid());
            response.put("remainingAmount", activePayment.getRemainingAmount());
            response.put("installments", installmentOptions);

            logger.info("Installment options for order {}: {}/{} completed",
                    orderId, completedInstallments, activePayment.getTotalInstallments());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to fetch installment options: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch installment options: " + e.getMessage()));
        }
    }

    /**
     * Get installment plan details with breakdown for a specific order
     */
    @GetMapping("/{orderId}/installment-plan-details")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getInstallmentPlanDetails(@PathVariable Long orderId, HttpServletRequest request) {
        logger.info("Fetching installment plan details for order ID: {}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            Payment activePayment = paymentService.getActivePaymentForOrder(order);

            if (activePayment == null || activePayment.getInstallmentPlanId() == null) {
                Map<String, Object> noInstallmentResponse = new HashMap<>();
                noInstallmentResponse.put("hasInstallmentPlan", false);
                return ResponseEntity.ok(noInstallmentResponse);
            }

            // Get the installment plan details from the available plans
            List<InstallmentPlanDTO> availablePlans = paymentService.getAvailableInstallmentPlansForOrder(orderId);
            InstallmentPlanDTO planDetails = availablePlans.stream()
                    .filter(plan -> plan.getId().equals(activePayment.getInstallmentPlanId().intValue()))
                    .findFirst()
                    .orElse(null);

            if (planDetails == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Installment plan details not found"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("hasInstallmentPlan", true);
            response.put("planDetails", planDetails);
            response.put("orderTotal", order.getTotalPrice());
            response.put("paymentId", activePayment.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to fetch installment plan details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch installment plan details: " + e.getMessage()));
        }
    }

    /**
     * EXISTING METHOD - Extract token from request (no changes)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("JWT Token is missing or invalid");
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

            // Log breakdown by payment method
            Map<String, Long> methodBreakdown = pendingPayments.stream()
                    .collect(Collectors.groupingBy(PaymentResponse::getMethod, Collectors.counting()));

            logger.info("Successfully fetched {} pending verification payments: {}",
                    pendingPayments.size(), methodBreakdown);

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

            // Log summary information
            double totalVerifiedAmount = verifiedPayments.stream()
                    .mapToDouble(PaymentResponse::getAmount)
                    .sum();

            logger.info("Successfully fetched {} recently verified payments totaling Rs. {}",
                    verifiedPayments.size(), totalVerifiedAmount);

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
            long count = installmentRepository.countPendingBankTransfers();
            logger.info("Pending payments count: {}", count);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Failed to fetch payment count: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch payment count: " + e.getMessage()));
        }
    }
}