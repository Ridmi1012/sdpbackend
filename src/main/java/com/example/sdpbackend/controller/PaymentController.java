package com.example.sdpbackend.controller;


import com.example.sdpbackend.dto.OrderResponse;
import com.example.sdpbackend.dto.PaymentRequest;
import com.example.sdpbackend.dto.PaymentResponse;
import com.example.sdpbackend.dto.PaymentSlipRequest;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.entity.Payment;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.repository.OrderRepository;
import com.example.sdpbackend.repository.PaymentRepository;
import com.example.sdpbackend.service.JWTService;
import com.example.sdpbackend.service.PaymentService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {
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

    @PostMapping("/payment")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        try {
            // Verify customer owns the order
            Long orderId = Long.valueOf(paymentRequest.getOrderId());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only make payments for your own orders"));
            }

            OrderResponse updatedOrder = paymentService.processPayment(paymentRequest);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to process payment: " + e.getMessage()));
        }
    }

    @PostMapping("/payhere/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initiatePayHerePayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        try {
            // Verify customer owns the order
            Long orderId = Long.valueOf(paymentRequest.getOrderId());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only make payments for your own orders"));
            }

            Map<String, String> response = paymentService.initiatePayHerePayment(paymentRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to initiate PayHere payment: " + e.getMessage()));
        }
    }

    @PostMapping("/payhere/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> verifyPayHerePayment(@RequestBody Map<String, String> verificationRequest, HttpServletRequest request) {
        try {
            String orderId = verificationRequest.get("orderId");
            String paymentId = verificationRequest.get("paymentId");

            // Verify customer owns the order
            Long orderIdLong = Long.valueOf(orderId);
            Order order = orderRepository.findById(orderIdLong)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only verify payments for your own orders"));
            }

            OrderResponse updatedOrder = paymentService.verifyPayHerePayment(orderId, paymentId);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to verify PayHere payment: " + e.getMessage()));
        }
    }

    @PostMapping("/payhere/notify")
    public ResponseEntity<?> payHereNotification(@RequestBody Map<String, String> notification) {
        try {
            String orderNumber = notification.get("order_id");
            String status = notification.get("status_code");
            String paymentId = notification.get("payment_id");
            String customParam = notification.get("custom_1");

            if ("2".equals(status)) {
                // Implement logic to update payment status
                // This is called by PayHere's server
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to process notification: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to handle payment slip uploads
     */
    @PostMapping("/{orderId}/payment-slip-upload")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> uploadPaymentSlip(
            @PathVariable Long orderId,
            @RequestBody PaymentSlipRequest paymentSlipRequest,
            HttpServletRequest request) {
        try {
            // Verify customer owns the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only upload payment slips for your own orders"));
            }

            // Use explicit amount from request if provided
            Double amount = paymentSlipRequest.getAmount();
            Boolean isPartialPayment = paymentSlipRequest.getIsPartialPayment();
            String notes = paymentSlipRequest.getNotes();

            OrderResponse updatedOrder = paymentService.uploadPaymentSlip(
                    orderId,
                    paymentSlipRequest.getImageUrl(),
                    amount,
                    isPartialPayment,
                    notes
            );

            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to upload payment slip: " + e.getMessage()));
        }
    }

    /**
     * Legacy endpoint for payment slip uploads maintained for compatibility
     */
    @PostMapping("/{orderId}/payment-slip")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> uploadPaymentSlipLegacy(
            @PathVariable("orderId") Long orderId,
            @RequestBody Map<String, String> paymentSlipData,
            HttpServletRequest request) {

        try {
            // Verify customer owns the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Get customer from username and verify ownership
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only upload payment slips for your own orders"));
            }

            // Extract image URL from request
            String imageUrl = paymentSlipData.get("imageUrl");
            if (imageUrl == null || imageUrl.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Image URL is required"));
            }

            OrderResponse response = paymentService.uploadPaymentSlip(orderId, imageUrl, null, null, null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to upload payment slip: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/payment/{paymentId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verifyManualPayment(
            @PathVariable Long orderId,
            @PathVariable Long paymentId,
            @RequestBody Map<String, Object> verificationRequest) {
        try {
            boolean isApproved = (boolean) verificationRequest.get("approved");
            String reason = verificationRequest.containsKey("reason") ?
                    (String) verificationRequest.get("reason") : null;

            OrderResponse updatedOrder = paymentService.verifyManualPayment(orderId, paymentId, isApproved, reason);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to verify payment: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/payments")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrderPayments(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            // Find the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            // If user is a customer, verify they can only access their own orders' payments
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                // Get customer from username and verify ownership
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(order.getCustomer().getcustomerId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access your own order payments"));
                }
            }

            List<Payment> payments = paymentRepository.findByOrderId(orderId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch payment details: " + e.getMessage()));
        }
    }

    @GetMapping("/pending/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingVerificationPayments() {
        try {
            List<PaymentResponse> pendingPayments = paymentService.getPendingPaymentsAsDTO();
            return ResponseEntity.ok(pendingPayments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch pending payments: " + e.getMessage()));
        }
    }

    @GetMapping("/payments/verified/recent") // Original path
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRecentlyVerifiedPayments() {
        try {
            List<PaymentResponse> verifiedPayments = paymentService.getRecentlyVerifiedPaymentsAsDTO();
            return ResponseEntity.ok(verifiedPayments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch recently verified payments: " + e.getMessage()));
        }
    }

    // Add this alternative endpoint to match your Angular service
    @GetMapping("/verified/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRecentVerifiedPayments() {
        try {
            List<PaymentResponse> verifiedPayments = paymentService.getRecentlyVerifiedPaymentsAsDTO();
            return ResponseEntity.ok(verifiedPayments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch recently verified payments: " + e.getMessage()));
        }
    }

    @GetMapping("/payments/pending/count") // More specific path
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingPaymentsCount() {
        try {
            long count = paymentRepository.countByStatus("pending");
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to fetch payment count: " + e.getMessage()));
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("JWT Token is missing or invalid");
    }
}
