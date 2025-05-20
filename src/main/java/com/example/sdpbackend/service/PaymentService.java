package com.example.sdpbackend.service;

import com.example.sdpbackend.config.PayHereConfig;
import com.example.sdpbackend.dto.*;
import com.example.sdpbackend.entity.*;
import com.example.sdpbackend.repository.InstallmentPlanRepository;
import com.example.sdpbackend.repository.InstallmentRepository;
import com.example.sdpbackend.repository.OrderRepository;
import com.example.sdpbackend.repository.PaymentRepository;
import com.example.sdpbackend.util.PaymentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PayHereConfig payHereConfig;

    /**
     * Process a payment for an order (for bank transfer)
     */
    @Transactional
    public OrderResponse processPayment(PaymentRequest paymentRequest) {
        logger.info("Processing payment for order ID: {}", paymentRequest.getOrderId());

        // Find the order
        Long orderId = Long.valueOf(paymentRequest.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // For PayHere, don't create payment record yet
        if ("payhere".equals(paymentRequest.getPaymentMethod())) {
            return initiatePayHerePayment(order, paymentRequest);
        }

        // For bank transfer, create payment record
        Payment payment = createOrUpdatePayment(order, paymentRequest);

        // Return order response
        return new OrderResponse();
    }

    private Payment createOrUpdatePayment(Order order, PaymentRequest paymentRequest) {
        // Check if order already has an active payment
        Payment existingPayment = order.getPayments().stream()
                .filter(p -> !"rejected".equals(p.getStatus()) && !"completed".equals(p.getStatus()))
                .findFirst()
                .orElse(null);

        Payment payment;

        if (existingPayment != null) {
            payment = existingPayment;
            logger.info("Using existing payment ID: {} for order: {}", payment.getId(), order.getId());
        } else {
            payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentMethod(paymentRequest.getPaymentMethod());

            // Set both totalAmount and amount fields
            double amountValue = paymentRequest.getAmount() != null ? paymentRequest.getAmount() : order.getTotalPrice();
            payment.setTotalAmount(order.getTotalPrice());
            payment.setAmount(amountValue); // Set the amount field correctly

            // Set payment type and plan
            if (paymentRequest.getInstallmentPlanId() != null && paymentRequest.getInstallmentPlanId() > 1) {
                payment.setPaymentType("installment");
                payment.setInstallmentPlanId(Long.valueOf(paymentRequest.getInstallmentPlanId()));

                InstallmentPlan plan = installmentPlanRepository.findById(payment.getInstallmentPlanId())
                        .orElseThrow(() -> new RuntimeException("Installment plan not found"));

                payment.setTotalInstallments(plan.getNumberOfInstallments());
                payment.setCurrentInstallment(1);
                createInstallmentRecords(payment, plan);
            } else {
                payment.setPaymentType("full");
                payment.setTotalInstallments(1);
                payment.setCurrentInstallment(1);
                payment.setInstallmentPlanId(1L); // Set to full payment plan ID

                Installment installment = new Installment();
                installment.setPayment(payment);
                installment.setInstallmentNumber(1);
                installment.setAmount(payment.getTotalAmount());
                installment.setPercentage(100.0);
                installment.setStatus("pending");
                installment.setPaymentMethod(paymentRequest.getPaymentMethod());
                installment.setNotes(paymentRequest.getNotes());

                // Set transaction ID if provided (for bank transfers)
                if (paymentRequest.getTransactionId() != null) {
                    installment.setTransactionId(paymentRequest.getTransactionId());
                }

                payment.getInstallments().add(installment);
            }

            payment.setStatus("pending");
            payment.setNotes(paymentRequest.getNotes());
            payment = paymentRepository.save(payment);
            logger.info("Created payment record with ID: {}", payment.getId());
        }

        return payment;
    }

    /**
     * Initiate PayHere payment without creating database records
     */
    private OrderResponse initiatePayHerePayment(Order order, PaymentRequest request) {
        logger.info("Initiating PayHere payment for order ID: {}", order.getId());

        // Calculate amount based on installment plan
        Double amount = request.getAmount();
        if (request.getInstallmentPlanId() != null && request.getInstallmentPlanId() > 1) {
            InstallmentPlan plan = installmentPlanRepository.findById(request.getInstallmentPlanId().longValue())
                    .orElseThrow(() -> new RuntimeException("Installment plan not found"));

            int installmentNumber = request.getInstallmentNumber() != null ? request.getInstallmentNumber() : 1;
            amount = (plan.getPercentages().get(installmentNumber - 1) / 100.0) * order.getTotalPrice();
        }

        // Build PayHere parameters without creating payment record
        Map<String, String> params = buildPayHereParametersForInitiation(order, request, amount);

        // Return order response with PayHere parameters
        OrderResponse response = new OrderResponse();
        response.setPayHereParams(params);
        return response;
    }

    /**
     * Handle PayHere payment verification - create records only on success
     */
    @Transactional
    public OrderResponse verifyPayHerePayment(String orderId, String paymentId) {
        logger.info("Verifying PayHere payment - Order: {}, PayHere Payment ID: {}", orderId, paymentId);

        Order order = orderRepository.findById(Long.valueOf(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = order.getPayments().stream()
                .filter(p -> !"rejected".equals(p.getStatus()) && !"completed".equals(p.getStatus()))
                .findFirst()
                .orElse(null);

        if (payment == null) {
            payment = createPaymentFromPayHereSuccess(order, paymentId);
        }

        Installment currentInstallment = payment.getCurrentInstallment();
        currentInstallment.setTransactionId(paymentId);
        currentInstallment.setStatus("confirmed");
        currentInstallment.setConfirmationDate(LocalDateTime.now());
        currentInstallment.setPaymentMethod("payhere");

        installmentRepository.save(currentInstallment);

        boolean shouldIncrementInstallment = payment.getCurrentInstallmentNumber() < payment.getTotalInstallments();
        payment.updateStatus();

        if (shouldIncrementInstallment && !"completed".equals(payment.getStatus())) {
            payment.setCurrentInstallment(payment.getCurrentInstallmentNumber() + 1);
        }

        paymentRepository.save(payment);
        updateOrderPaymentStatus(order, payment);
        notificationService.createPaymentNotification(order, payment);

        return new OrderResponse();
    }

    // Update in createPaymentFromPayHereSuccess method
    private Payment createPaymentFromPayHereSuccess(Order order, String paymentId) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("payhere");
        payment.setTotalAmount(order.getTotalPrice());
        payment.setAmount(order.getTotalPrice()); // Set amount field correctly

        payment.setPaymentType("full");
        payment.setTotalInstallments(1);
        payment.setCurrentInstallment(1);
        payment.setStatus("pending");
        payment.setInstallmentPlanId(1L); // Full payment plan

        Installment installment = new Installment();
        installment.setPayment(payment);
        installment.setInstallmentNumber(1);
        installment.setAmount(payment.getTotalAmount());
        installment.setPercentage(100.0);
        installment.setStatus("pending");
        installment.setPaymentMethod("payhere");

        payment.getInstallments().add(installment);

        return paymentRepository.save(payment);
    }

    // Update in uploadPaymentSlip method to handle amount correctly
    @Transactional
    public OrderResponse uploadPaymentSlip(Long orderId, String imageUrl, Double amount,
                                           Boolean isPartialPayment, String notes) {
        logger.info("Processing payment slip upload for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = order.getPayments().stream()
                .filter(p -> !"rejected".equals(p.getStatus()) && !"completed".equals(p.getStatus()))
                .findFirst()
                .orElse(null);

        if (payment == null) {
            PaymentRequest request = new PaymentRequest();
            request.setOrderId(orderId.toString());
            request.setPaymentMethod("bank-transfer");
            request.setAmount(amount);
            request.setNotes(notes);
            payment = createOrUpdatePayment(order, request);
        } else {
            // If the payment already exists, make sure the amount field is set
            if (payment.getAmount() == null) {
                payment.setAmount(amount);
                payment = paymentRepository.save(payment);
            }
        }

        Installment currentInstallment = payment.getCurrentInstallment();
        if (currentInstallment == null) {
            throw new RuntimeException("No current installment found");
        }

        if (currentInstallment.getStatus().equals("confirmed")) {
            throw new RuntimeException("Current installment is already paid");
        }

        currentInstallment.setPaymentSlipUrl(imageUrl);
        currentInstallment.setNotes(notes);
        currentInstallment.setPaymentMethod("bank-transfer");
        currentInstallment.setStatus("pending");
        currentInstallment.setAmount(amount);

        // Generate a transaction ID for bank transfers
        String transactionId = "BT-" + System.currentTimeMillis();
        currentInstallment.setTransactionId(transactionId);

        installmentRepository.save(currentInstallment);

        notificationService.createPaymentSlipNotification(order, payment);

        return new OrderResponse();
    }

    /**
     * Verify manual payment
     */
    @Transactional
    public OrderResponse verifyManualPayment(Long orderId, Long paymentId, boolean isApproved, String reason) {
        logger.info("Verifying manual payment - Order: {}, Payment: {}, Approved: {}",
                orderId, paymentId, isApproved);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Installment currentInstallment = payment.getCurrentInstallment();
        if (currentInstallment == null || !"pending".equals(currentInstallment.getStatus())) {
            throw new RuntimeException("No pending installment found for verification");
        }

        // Get the current user's username from security context
        String adminUsername = getCurrentUsername();

        if (isApproved) {
            currentInstallment.setStatus("confirmed");
            currentInstallment.setConfirmationDate(LocalDateTime.now());
            currentInstallment.setVerifiedBy(adminUsername);

            installmentRepository.save(currentInstallment);

            boolean shouldIncrementInstallment = payment.getCurrentInstallmentNumber() < payment.getTotalInstallments();
            payment.updateStatus();

            if (shouldIncrementInstallment && !"completed".equals(payment.getStatus())) {
                payment.setCurrentInstallment(payment.getCurrentInstallmentNumber() + 1);
            }
        } else {
            currentInstallment.setStatus("rejected");
            currentInstallment.setRejectionReason(reason);
            currentInstallment.setVerifiedBy(adminUsername);
            installmentRepository.save(currentInstallment);

            payment.updateStatus();
        }

        paymentRepository.save(payment);
        updateOrderPaymentStatus(order, payment);
        notificationService.createPaymentVerificationNotification(order, payment, isApproved);

        return new OrderResponse();
    }

    /**
     * Get current username from security context
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            return authentication.getName();
        }
        return "system";
    }

    /**
     * Build PayHere parameters for payment initiation
     */
    private Map<String, String> buildPayHereParametersForInitiation(Order order, PaymentRequest request, Double amount) {
        Map<String, String> params = new HashMap<>();

        params.put("merchant_id", payHereConfig.getMerchantId());
        params.put("return_url", payHereConfig.getReturnUrl());
        params.put("cancel_url", payHereConfig.getCancelUrl());
        params.put("notify_url", payHereConfig.getNotifyUrl());

        params.put("order_id", order.getId().toString());
        params.put("items", "Order #" + order.getOrderNumber());
        params.put("currency", "LKR");
        params.put("amount", String.valueOf(amount));

        // Customer details
        Customer customer = order.getCustomer();
        params.put("first_name", customer.getFirstName());
        params.put("last_name", customer.getLastName());
        params.put("email", customer.getEmail());
        params.put("phone", customer.getContact());
        params.put("address", "");
        params.put("city", "");
        params.put("country", "Sri Lanka");

        // Custom fields for later reference
        params.put("custom_1", String.valueOf(request.getInstallmentPlanId() != null ? request.getInstallmentPlanId() : 1));
        params.put("custom_2", String.valueOf(request.getInstallmentNumber() != null ? request.getInstallmentNumber() : 1));

        return params;
    }

    /**
     * Create installment records based on plan
     */
    private void createInstallmentRecords(Payment payment, InstallmentPlan plan) {
        for (int i = 0; i < plan.getNumberOfInstallments(); i++) {
            Installment installment = new Installment();
            installment.setPayment(payment);
            installment.setInstallmentNumber(i + 1);
            installment.setPercentage(plan.getPercentages().get(i));
            installment.setAmount((plan.getPercentages().get(i) / 100.0) * payment.getTotalAmount());
            installment.setStatus("pending");
            installment.setPaymentMethod(payment.getPaymentMethod());

            payment.getInstallments().add(installment);
        }
    }

    /**
     * Get payment summary
     */
    public PaymentSummaryDTO getPaymentSummary(Long orderId) {
        logger.info("Getting payment summary for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        PaymentSummaryDTO summary = new PaymentSummaryDTO();
        summary.setTotalAmount(order.getTotalPrice());

        // Get active payment
        Payment activePayment = order.getPayments().stream()
                .filter(p -> !"rejected".equals(p.getStatus()))
                .findFirst()
                .orElse(null);

        if (activePayment != null) {
            summary.setTotalPaid(activePayment.getTotalPaid());
            summary.setRemainingAmount(activePayment.getRemainingAmount());
            summary.setIsFullyPaid(activePayment.isFullyPaid());
            summary.setPaymentStatus(activePayment.getStatus());
            summary.setTotalInstallments(activePayment.getTotalInstallments());

            // Convert installments to payment responses
            List<PaymentResponse> paymentResponses = activePayment.getInstallments().stream()
                    .map(installment -> paymentMapper.installmentToPaymentResponse(installment))
                    .collect(Collectors.toList());
            summary.setPayments(paymentResponses);

            // Set installment plan details
            if ("installment".equals(activePayment.getPaymentType())) {
                summary.setCurrentInstallment(activePayment.getCurrentInstallmentNumber());

                // Set installment plan details
                if (activePayment.getInstallmentPlanId() != null) {
                    InstallmentPlan plan = installmentPlanRepository.findById(activePayment.getInstallmentPlanId())
                            .orElse(null);
                    if (plan != null) {
                        InstallmentPlan summaryPlan = new InstallmentPlan();
                        summaryPlan.setId(plan.getId());
                        summaryPlan.setName(plan.getName());
                        summaryPlan.setNumberOfInstallments(plan.getNumberOfInstallments());
                        summaryPlan.setPercentages(plan.getPercentages());
                        summaryPlan.setDescription(plan.getDescription());
                        summary.setInstallmentPlan(summaryPlan);
                    }
                }

                Installment nextInstallment = activePayment.getInstallments().stream()
                        .filter(i -> i.getInstallmentNumber().equals(activePayment.getCurrentInstallmentNumber()))
                        .findFirst()
                        .orElse(null);

                if (nextInstallment != null && !"confirmed".equals(nextInstallment.getStatus())) {
                    summary.setNextInstallmentAmount(nextInstallment.getAmount());
                }
            }

            // Set payment ID
            summary.setActivePaymentId(activePayment.getId());
        } else {
            summary.setTotalPaid(0.0);
            summary.setRemainingAmount(order.getTotalPrice());
            summary.setIsFullyPaid(false);
            summary.setPaymentStatus("pending");
            summary.setPayments(new ArrayList<>());
        }

        // Calculate deadline date (12 hours before event)
        if (order.getEventDetails() != null && order.getEventDetails().getEventDate() != null) {
            LocalDateTime eventDate = LocalDateTime.parse(order.getEventDetails().getEventDate());
            LocalDateTime deadline = eventDate.minusHours(12);
            summary.setDeadlineDate(deadline.toString());
        }

        return summary;
    }

    /**
     * Update order payment status based on payment
     */
    private void updateOrderPaymentStatus(Order order, Payment payment) {
        if (payment.isFullyPaid()) {
            order.setPaymentStatus("completed");
        } else if (payment.getTotalPaid() > 0) {
            order.setPaymentStatus("partial");
        } else {
            order.setPaymentStatus("pending");
        }
        orderRepository.save(order);
    }

    /**
     * Get available installment plans for an order
     */
    public List<InstallmentPlanDTO> getAvailableInstallmentPlansForOrder(Long orderId) {
        logger.info("Getting available installment plans for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check event date to filter plans
        LocalDateTime eventDate = LocalDateTime.now().plusDays(30);
        if (order.getEventDetails() != null && order.getEventDetails().getEventDate() != null) {
            String eventDateStr = order.getEventDetails().getEventDate();
            try {
                // Try to parse as LocalDateTime first (if it contains time)
                if (eventDateStr.contains("T")) {
                    eventDate = LocalDateTime.parse(eventDateStr);
                } else {
                    // Parse as LocalDate and convert to LocalDateTime
                    LocalDate localDate = LocalDate.parse(eventDateStr);
                    eventDate = localDate.atStartOfDay();
                }
            } catch (Exception e) {
                logger.error("Error parsing event date: {}", eventDateStr, e);
                // Fall back to default
                eventDate = LocalDateTime.now().plusDays(30);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        long daysUntilEvent = java.time.Duration.between(now, eventDate).toDays();

        // Get all active plans
        List<InstallmentPlan> plans = installmentPlanRepository.findAll().stream()
                .filter(InstallmentPlan::getIsActive)
                .collect(Collectors.toList());

        // Filter based on days until event
        if (daysUntilEvent <= 10) {
            plans = plans.stream()
                    .filter(p -> p.getNumberOfInstallments() == 1)
                    .collect(Collectors.toList());
        } else if (daysUntilEvent <= 30) {
            plans = plans.stream()
                    .filter(p -> p.getNumberOfInstallments() <= 2)
                    .collect(Collectors.toList());
        }

        // Convert to DTOs
        return plans.stream()
                .map(this::convertToInstallmentPlanDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert InstallmentPlan to DTO
     */
    private InstallmentPlanDTO convertToInstallmentPlanDTO(InstallmentPlan plan) {
        InstallmentPlanDTO dto = new InstallmentPlanDTO();
        dto.setId(plan.getId().intValue());
        dto.setName(plan.getName());
        dto.setNumberOfInstallments(plan.getNumberOfInstallments());
        dto.setPercentages(plan.getPercentages());
        dto.setDescription(plan.getDescription());
        return dto;
    }

    /**
     * Get pending payments as DTOs
     */
    public List<PaymentResponse> getPendingPaymentsAsDTO() {
        logger.info("Fetching pending payments for verification");

        List<Installment> pendingInstallments = installmentRepository.findAll().stream()
                .filter(i -> "pending".equals(i.getStatus()) && !"payhere".equals(i.getPaymentMethod()))
                .collect(Collectors.toList());

        return pendingInstallments.stream()
                .map(installment -> paymentMapper.installmentToPaymentResponse(installment))
                .collect(Collectors.toList());
    }

    /**
     * Get recently verified payments as DTOs
     */
    public List<PaymentResponse> getRecentlyVerifiedPaymentsAsDTO() {
        logger.info("Fetching recently verified payments");

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Installment> recentInstallments = installmentRepository.findAll().stream()
                .filter(i -> "confirmed".equals(i.getStatus()) &&
                        i.getConfirmationDate() != null &&
                        i.getConfirmationDate().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());

        return recentInstallments.stream()
                .map(installment -> paymentMapper.installmentToPaymentResponse(installment))
                .collect(Collectors.toList());
    }

    /**
     * Get next installment info for an order
     */
    public Map<String, Object> getNextInstallmentInfo(Long orderId) {
        logger.info("Getting next installment info for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Map<String, Object> result = new HashMap<>();

        Payment activePayment = order.getPayments().stream()
                .filter(p -> !"rejected".equals(p.getStatus()))
                .findFirst()
                .orElse(null);

        if (activePayment == null || !"installment".equals(activePayment.getPaymentType())) {
            result.put("hasInstallmentPlan", false);
            result.put("message", "No installment plan found for this order");
            return result;
        }

        Integer currentInstallmentNumber = activePayment.getCurrentInstallmentNumber();
        result.put("currentInstallmentNumber", currentInstallmentNumber);
        result.put("totalInstallments", activePayment.getTotalInstallments());

        if (currentInstallmentNumber > activePayment.getTotalInstallments()) {
            result.put("allPaid", true);
            result.put("message", "All installments have been paid");
            return result;
        }

        Installment nextInstallment = activePayment.getInstallments().stream()
                .filter(i -> i.getInstallmentNumber().equals(currentInstallmentNumber))
                .findFirst()
                .orElse(null);

        if (nextInstallment != null) {
            result.put("nextInstallmentAmount", nextInstallment.getAmount());
            result.put("nextInstallmentPercentage", nextInstallment.getPercentage());
            result.put("status", nextInstallment.getStatus());
        }

        return result;
    }

    /**
     * Get payment mapper
     */
    public PaymentMapper getPaymentMapper() {
        return paymentMapper;
    }
}