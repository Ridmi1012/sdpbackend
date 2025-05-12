package com.example.sdpbackend.service;

import com.example.sdpbackend.config.PayHereConfig;
import com.example.sdpbackend.dto.*;

import com.example.sdpbackend.entity.InstallmentPlan;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.entity.Payment;
import com.example.sdpbackend.repository.InstallmentPlanRepository;
import com.example.sdpbackend.repository.OrderRepository;
import com.example.sdpbackend.repository.PaymentRepository;
import com.example.sdpbackend.util.OrderMapper;
import com.example.sdpbackend.util.PayHereVerifier;
import com.example.sdpbackend.util.PaymentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private EventService eventService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PayHereConfig payHereConfig;

    @Autowired
    private PayHereVerifier payHereVerifier;


    /**
     * Process a payment for an order
     * Handles both PayHere and bank transfer methods
     */
//    @Transactional
//    public OrderResponse processPayment(PaymentRequest paymentRequest) {
//        logger.info("Processing payment for order ID: {}", paymentRequest.getOrderId());
//
//        // Find the order
//        Long orderId = Long.valueOf(paymentRequest.getOrderId());
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> {
//                    logger.error("Order not found with id: {}", orderId);
//                    return new RuntimeException("Order not found with id: " + orderId);
//                });
//
//        // Verify that partial payments are only allowed for events > 24 hours away
//        if (paymentRequest.getAmount() < order.getTotalPrice() && isEventWithin24Hours(order)) {
//            logger.warn("Attempted partial payment for order with event within 24 hours: {}", orderId);
//            throw new RuntimeException("Full payment is required for events within 24 hours");
//        }
//
//        // Create and save payment record
//        Payment payment = new Payment();
//        payment.setAmount(paymentRequest.getAmount());
//        payment.setMethod(paymentRequest.getPaymentMethod());
//        payment.setTransactionId(paymentRequest.getTransactionId());
//        payment.setStatus("completed");
//        payment.setOrder(order);
//        payment.setConfirmationDateTime(LocalDateTime.now());
//
//        // Handle installment tracking
//        setInstallmentInfo(payment, order, paymentRequest);
//
//        // Calculate payment amounts and determine if partial
//        calculatePaymentAmounts(payment, order);
//
//        // Add payment to order's payment list
//        order.getPayments().add(payment);
//        payment = paymentRepository.save(payment);
//        logger.info("Created payment record with ID: {}", payment.getId());
//
//        // Update order payment status
//        updateOrderPaymentStatus(order);
//
//        // Update event calendar
//        updateEventAfterPayment(order);
//
//        // Save order with updated status
//        Order savedOrder = orderRepository.save(order);
//
//        // Create notification about the payment
//        try {
//            notificationService.createPaymentNotification(savedOrder, payment);
//            logger.info("Payment notification created for order: {}", order.getId());
//        } catch (Exception e) {
//            logger.error("Error creating payment notification: {}", e.getMessage(), e);
//            // Continue processing as this is non-critical
//        }
//
//        return new OrderResponse(); // Convert order to OrderResponse using mapper
//    }

    /**
     * Upload payment slip for bank transfer
     */
    /**
     * Upload payment slip for bank transfer
     * CHANGE: Implement is_active flag properly
     */
    @Transactional
    public OrderResponse uploadPaymentSlip(
            Long orderId,
            String imageUrl,
            Double amount,
            Boolean isPartialPayment,
            String notes
    ) {
        logger.info("Processing payment slip upload for order ID: {}", orderId);

        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with id: {}", orderId);
                    return new RuntimeException("Order not found with id: " + orderId);
                });

        // Verify that partial payments are only allowed for events > 24 hours away
        if (isPartialPayment && isEventWithin24Hours(order)) {
            logger.warn("Attempted partial payment slip upload for order with event within 24 hours: {}", orderId);
            throw new RuntimeException("Full payment is required for events within 24 hours");
        }

        // Create new pending payment record
        Payment payment = new Payment();

        // Set payment amount
        if (amount != null) {
            payment.setAmount(amount);
        } else {
            payment.setAmount(order.getTotalPrice() != null ? order.getTotalPrice() : 0.0);
        }

        payment.setMethod("bank-transfer");
        payment.setPaymentSlipUrl(imageUrl);
        payment.setStatus("pending"); // Needs admin verification
        payment.setOrder(order);
        payment.setIsPartialPayment(isPartialPayment != null ? isPartialPayment : false);

        // Set installment tracking info if applicable
        if (order.getInstallmentPlanId() != null) {
            payment.setInstallmentPlanId(order.getInstallmentPlanId().intValue());
            payment.setInstallmentNumber(order.getCurrentInstallmentNumber());
        }

        if (notes != null && !notes.isEmpty()) {
            payment.setNotes(notes);
        }

        // Calculate remaining amount
        calculatePaymentAmounts(payment, order);

        // Mark as active since this is the most recent payment attempt
        // (will still need admin verification)
        deactivateAllPaymentsForOrder(order);
        payment.setIsActive(true);

        // Add payment to order
        order.getPayments().add(payment);
        payment = paymentRepository.save(payment);
        logger.info("Created pending payment record with ID: {}", payment.getId());

        // Update order payment status to pending
        order.setPaymentStatus("pending");

        // Set next installment due date if needed
        setNextInstallmentDueDate(order);

        // Save order with updated status
        Order savedOrder = orderRepository.save(order);

        // Create notification for admin
        try {
            notificationService.createPaymentSlipNotification(savedOrder, payment);
            logger.info("Payment slip notification created for admin");
        } catch (Exception e) {
            logger.error("Error creating payment slip notification: {}", e.getMessage(), e);
        }

        return new OrderResponse(); // Convert order to OrderResponse using mapper
    }

//    /**
//     * Verify a manual payment (admin)
//     */
//    @Transactional
//    public OrderResponse verifyManualPayment(Long orderId, Long paymentId, boolean isApproved, String reason) {
//        logger.info("Verifying manual payment - Order: {}, Payment: {}, Approved: {}",
//                orderId, paymentId, isApproved);
//
//        // Find the order
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> {
//                    logger.error("Order not found with id: {}", orderId);
//                    return new RuntimeException("Order not found with id: " + orderId);
//                });
//
//        // Find the payment
//        Payment payment = paymentRepository.findById(paymentId)
//                .orElseThrow(() -> {
//                    logger.error("Payment not found with id: {}", paymentId);
//                    return new RuntimeException("Payment not found with id: " + paymentId);
//                });
//
//        if (isApproved) {
//            // Approve payment
//            payment.setStatus("completed");
//            payment.setConfirmationDateTime(LocalDateTime.now());
//            logger.info("Payment approved: {}", payment.getId());
//
//            // Update order payment status
//            updateOrderPaymentStatus(order);
//
//            // Update event in calendar
//            updateEventAfterPayment(order);
//        } else {
//            // Reject payment
//            payment.setStatus("rejected");
//            if (reason != null && !reason.isEmpty()) {
//                payment.setRejectionReason(reason);
//            }
//            logger.info("Payment rejected: {}, reason: {}", payment.getId(), reason);
//        }
//
//        // Save payment and order
//        paymentRepository.save(payment);
//        Order savedOrder = orderRepository.save(order);
//
//        // Notify customer about payment verification
//        try {
//            notificationService.createPaymentVerificationNotification(savedOrder, payment, isApproved);
//            logger.info("Payment verification notification sent to customer");
//        } catch (Exception e) {
//            logger.error("Error creating verification notification: {}", e.getMessage(), e);
//            // Continue processing as this is non-critical
//        }
//
//        return new OrderResponse(); // Convert order to OrderResponse using mapper
//    }

    /**
     * Get payment summary for an order
     * CHANGE: Include is_active in the response
     */
    public PaymentSummaryDTO getPaymentSummary(Long orderId) {
        logger.info("Calculating payment summary for order ID: {}", orderId);

        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with id: {}", orderId);
                    return new RuntimeException("Order not found with id: " + orderId);
                });

        // Create payment summary DTO
        PaymentSummaryDTO summary = new PaymentSummaryDTO();

        // Set total amount
        summary.setTotalAmount(order.getTotalPrice() != null ? order.getTotalPrice() : 0.0);

        // Calculate total paid from completed payments
        Double totalPaid = order.getPayments().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();
        summary.setTotalPaid(totalPaid);

        // Calculate remaining amount
        Double remainingAmount = Math.max(0, summary.getTotalAmount() - totalPaid);
        summary.setRemainingAmount(remainingAmount);

        // Check if fully paid
        summary.setIsFullyPaid(remainingAmount == 0 && totalPaid > 0);

        // Set payment status
        summary.setPaymentStatus(order.getPaymentStatus());

        // Get all payments and map to DTOs
        List<PaymentResponse> payments = order.getPayments().stream()
                .map(paymentMapper::toDTO)
                .collect(Collectors.toList());
        summary.setPayments(payments);

        // Calculate payment deadline
        setPaymentDeadline(summary, order);

        // Add installment plan details if applicable
        addInstallmentPlanDetails(summary, order);

        // Find the active payment (most recent successful or pending)
        Payment activePayment = order.getPayments().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .findFirst().orElse(null);

        if (activePayment != null) {
            logger.debug("Found active payment with ID: {} for order ID: {}", activePayment.getId(), orderId);
            summary.setActivePaymentId(activePayment.getId());
        } else {
            logger.debug("No active payment found for order ID: {}", orderId);
            // Set to null explicitly to ensure consistent behavior
            summary.setActivePaymentId(null);
        }

        return summary;
    }

    /**
     * Get available installment plans for an order
     */
    public List<InstallmentPlanDTO> getAvailableInstallmentPlansForOrder(Long orderId) {
        logger.info("Getting available installment plans for order ID: {}", orderId);

        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with id: {}", orderId);
                    return new RuntimeException("Order not found with id: " + orderId);
                });

        // Check if event date allows installment plans
        if (order.getEventDetails() != null && order.getEventDetails().getEventDate() != null) {
            try {
                // Parse event date
                LocalDate eventDate = LocalDate.parse(order.getEventDetails().getEventDate());
                LocalDate today = LocalDate.now();

                // Calculate days until event
                long daysUntilEvent = ChronoUnit.DAYS.between(today, eventDate);

                // Get appropriate plans based on time until event
                List<InstallmentPlan> plans;

                if (daysUntilEvent <= 10) {
                    // Only full payment for events within 10 days
                    plans = installmentPlanRepository.findAll().stream()
                            .filter(p -> p.getNumberOfInstallments() == 1)
                            .collect(Collectors.toList());
                    logger.info("Event is within 10 days - returning only full payment option");
                } else {
                    // All active plans for events more than 10 days away
                    plans = installmentPlanRepository.findAll().stream()
                            .filter(InstallmentPlan::getIsActive)
                            .collect(Collectors.toList());
                    logger.info("Event is more than 10 days away - returning all installment plans");
                }

                // Convert to DTOs and return
                return plans.stream()
                        .map(this::convertToPlanDTO)
                        .collect(Collectors.toList());

            } catch (Exception e) {
                logger.error("Error parsing event date or retrieving plans: {}", e.getMessage());
                // On error, return only full payment option
            }
        }

        // Fallback to only full payment option
        try {
            InstallmentPlan fullPaymentPlan = installmentPlanRepository.findByName("Full Payment");
            if (fullPaymentPlan != null) {
                return List.of(convertToPlanDTO(fullPaymentPlan));
            }
        } catch (Exception e) {
            logger.error("Error retrieving default full payment plan: {}", e.getMessage());
        }

        // Ultimate fallback - create a default plan DTO
        InstallmentPlanDTO defaultPlan = new InstallmentPlanDTO();
        defaultPlan.setId(1);
        defaultPlan.setName("Full Payment");
        defaultPlan.setNumberOfInstallments(1);
        defaultPlan.setPercentages(List.of(100.0));
        defaultPlan.setDescription("Pay the full amount in one payment");

        return List.of(defaultPlan);
    }

    // Helper methods

    /**
     * Convert InstallmentPlan entity to DTO
     */
    private InstallmentPlanDTO convertToPlanDTO(InstallmentPlan plan) {
        InstallmentPlanDTO dto = new InstallmentPlanDTO();
        dto.setId(plan.getId().intValue());
        dto.setName(plan.getName());
        dto.setNumberOfInstallments(plan.getNumberOfInstallments());
        dto.setPercentages(plan.getPercentages());
        dto.setDescription(plan.getDescription());
        return dto;
    }

    /**
     * Set installment information on payment
     */
    private void setInstallmentInfo(Payment payment, Order order, PaymentRequest paymentRequest) {
        // Set installment info if provided
        if (paymentRequest.getInstallmentPlanId() != null) {
            payment.setInstallmentPlanId(paymentRequest.getInstallmentPlanId());

            // Update order's installment plan if not already set
            if (order.getInstallmentPlanId() == null) {
                order.setInstallmentPlanId(paymentRequest.getInstallmentPlanId().longValue());

                // Get plan details to set total installments
                try {
                    InstallmentPlan plan = installmentPlanRepository.findById(
                                    paymentRequest.getInstallmentPlanId().longValue())
                            .orElse(null);

                    if (plan != null) {
                        order.setInstallmentTotalInstallments(plan.getNumberOfInstallments());
                    }
                } catch (Exception e) {
                    logger.error("Error retrieving installment plan: {}", e.getMessage());
                }
            }
        }

        if (paymentRequest.getInstallmentNumber() != null) {
            payment.setInstallmentNumber(paymentRequest.getInstallmentNumber());
        }

        if (paymentRequest.getNotes() != null) {
            payment.setNotes(paymentRequest.getNotes());
        }
    }

    /**
     * Calculate payment amounts
     */
    private void calculatePaymentAmounts(Payment payment, Order order) {
        // Calculate total amount paid so far
        Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
        Double totalPaid = order.getPayments().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        // Calculate remaining after this payment
        payment.setRemainingAmount(Math.max(0, totalPrice - totalPaid - payment.getAmount()));

        // Determine if this is a partial payment
        payment.setIsPartialPayment(totalPaid + payment.getAmount() < totalPrice);

        logger.debug("Payment amounts - Total: {}, Paid: {}, This payment: {}, Remaining: {}, Partial: {}",
                totalPrice, totalPaid, payment.getAmount(), payment.getRemainingAmount(),
                payment.getIsPartialPayment());
    }

    /**
     * Update order payment status
     */
    private void updateOrderPaymentStatus(Order order) {
        // Calculate total price and total paid
        Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
        Double totalPaid = order.getPayments().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        // Update status based on payment amounts
        if (totalPaid >= totalPrice) {
            order.setPaymentStatus("completed");
            logger.info("Order payment status updated to 'completed'");
        } else if (totalPaid > 0) {
            order.setPaymentStatus("partial");
            logger.info("Order payment status updated to 'partial'");

            // Update installment tracking if needed
            updateInstallmentTracking(order);
        } else {
            order.setPaymentStatus("pending");
            logger.info("Order payment status remains 'pending'");
        }
    }

    /**
     * Update installment tracking
     */
    private void updateInstallmentTracking(Order order) {
        if (order.getInstallmentPlanId() != null) {
            // Find the latest completed payment with installment number
            Payment latestPayment = order.getPayments().stream()
                    .filter(p -> "completed".equals(p.getStatus()) && p.getInstallmentNumber() != null)
                    .max((p1, p2) -> p1.getInstallmentNumber().compareTo(p2.getInstallmentNumber()))
                    .orElse(null);

            if (latestPayment != null) {
                // Increment to next installment
                order.setCurrentInstallmentNumber(latestPayment.getInstallmentNumber() + 1);

                // Set next installment due date
                setNextInstallmentDueDate(order);

                logger.info("Updated installment tracking - Current: {}, Total: {}",
                        order.getCurrentInstallmentNumber(), order.getInstallmentTotalInstallments());
            }
        }
    }

    /**
     * Set next installment due date
     */
    private void setNextInstallmentDueDate(Order order) {
        if (order.getEventDetails() != null && order.getEventDetails().getEventDate() != null) {
            try {
                LocalDate eventDate = LocalDate.parse(order.getEventDetails().getEventDate());
                LocalDate today = LocalDate.now();
                long daysUntilEvent = ChronoUnit.DAYS.between(today, eventDate);

                if (daysUntilEvent > 5) {
                    // Set due date to halfway between now and event
                    long halfwayDays = Math.max(daysUntilEvent / 2, 2); // At least 2 days ahead
                    LocalDateTime dueDate = LocalDateTime.now().plusDays(halfwayDays);
                    order.setNextInstallmentDueDate(dueDate);
                    logger.debug("Next installment due date set to {} days from now", halfwayDays);
                } else {
                    // If event is very soon, make payment due tomorrow
                    order.setNextInstallmentDueDate(LocalDateTime.now().plusDays(1));
                    logger.debug("Event is soon, next installment due tomorrow");
                }
            } catch (Exception e) {
                logger.error("Error calculating next installment due date: {}", e.getMessage());
                // Default to 3 days from now
                order.setNextInstallmentDueDate(LocalDateTime.now().plusDays(3));
            }
        }
    }

    /**
     * Update event calendar after payment
     */
    private void updateEventAfterPayment(Order order) {
        boolean isFullyPaid = "completed".equals(order.getPaymentStatus());

        try {
            eventService.createOrUpdateEventFromOrder(order, isFullyPaid);
            logger.info("Event calendar updated with payment status for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Error updating event calendar: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if event is within 24 hours
     */
    private boolean isEventWithin24Hours(Order order) {
        if (order.getEventDetails() != null && order.getEventDetails().getEventDate() != null) {
            try {
                // Parse the event date
                String eventDateStr = order.getEventDetails().getEventDate();
                LocalDate eventDate = LocalDate.parse(eventDateStr);
                LocalDateTime eventDateTime = eventDate.atStartOfDay();

                // If event time is provided, use it
                if (order.getEventDetails().getEventTime() != null) {
                    String[] timeParts = order.getEventDetails().getEventTime().split(":");
                    int hours = Integer.parseInt(timeParts[0]);
                    int minutes = Integer.parseInt(timeParts[1]);
                    eventDateTime = eventDate.atTime(hours, minutes);
                }

                // Check if event is within 24 hours
                LocalDateTime now = LocalDateTime.now();
                long hoursUntilEvent = ChronoUnit.HOURS.between(now, eventDateTime);

                return hoursUntilEvent < 24;
            } catch (Exception e) {
                logger.error("Error parsing event date/time: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Set payment deadline in the summary DTO
     */
    private void setPaymentDeadline(PaymentSummaryDTO summary, Order order) {
        if (!summary.getIsFullyPaid()) {
            if (order.getNextInstallmentDueDate() != null) {
                // Use the stored next installment due date
                summary.setNextInstallmentDueDate(
                        order.getNextInstallmentDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
            }

            // Calculate final deadline (24 hours before event)
            if (order.getEventDetails() != null && order.getEventDetails().getEventDate() != null) {
                try {
                    // Parse event date
                    LocalDate eventDate = LocalDate.parse(order.getEventDetails().getEventDate());
                    LocalDateTime eventDateTime = eventDate.atStartOfDay();

                    // If event time is specified, use it
                    if (order.getEventDetails().getEventTime() != null) {
                        try {
                            String[] timeParts = order.getEventDetails().getEventTime().split(":");
                            int hours = Integer.parseInt(timeParts[0]);
                            int minutes = Integer.parseInt(timeParts[1]);
                            eventDateTime = eventDate.atTime(hours, minutes);
                        } catch (Exception e) {
                            logger.error("Error parsing event time: {}", e.getMessage());
                        }
                    }

                    // Set deadline to 24 hours before event
                    LocalDateTime deadlineDateTime = eventDateTime.minusHours(24);
                    summary.setDeadlineDate(deadlineDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    logger.debug("Payment deadline set to 24 hours before event: {}", summary.getDeadlineDate());
                } catch (Exception e) {
                    logger.error("Error calculating payment deadline: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Add installment plan details to payment summary
     */
    private void addInstallmentPlanDetails(PaymentSummaryDTO summary, Order order) {
        if (order.getInstallmentPlanId() != null) {
            try {
                InstallmentPlan plan = installmentPlanRepository.findById(order.getInstallmentPlanId())
                        .orElse(null);

                if (plan != null) {
                    Map<String, Object> installmentPlanDetails = new HashMap<>();
                    installmentPlanDetails.put("id", plan.getId());
                    installmentPlanDetails.put("name", plan.getName());
                    installmentPlanDetails.put("numberOfInstallments", plan.getNumberOfInstallments());
                    installmentPlanDetails.put("percentages", plan.getPercentages());
                    installmentPlanDetails.put("description", plan.getDescription());

                    summary.setInstallmentPlan(installmentPlanDetails);
                    summary.setCurrentInstallment(order.getCurrentInstallmentNumber());

                    // Calculate next installment amount if not fully paid
                    if (!summary.getIsFullyPaid() &&
                            order.getCurrentInstallmentNumber() <= plan.getNumberOfInstallments()) {
                        int currentInstallment = order.getCurrentInstallmentNumber();
                        int installmentIndex = currentInstallment - 1;

                        if (installmentIndex >= 0 && installmentIndex < plan.getPercentages().size()) {
                            Double percentage = plan.getPercentages().get(installmentIndex);
                            Double amount = (percentage / 100.0) * summary.getTotalAmount();
                            summary.setNextInstallmentAmount(amount);
                            logger.debug("Next installment amount: {}", amount);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error getting installment plan details: {}", e.getMessage());
            }
        }
    }

    /**
     * Get all pending payments that need admin verification
     */
    public List<PaymentResponse> getPendingPaymentsAsDTO() {
        logger.info("Fetching pending payments for verification");

        // Exclude PayHere payments which are verified automatically
        List<Payment> pendingPayments = paymentRepository.findByStatusAndMethodNot("pending", "payhere");

        return pendingPayments.stream()
                .map(paymentMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get recently verified payments
     */
    public List<PaymentResponse> getRecentlyVerifiedPaymentsAsDTO() {
        logger.info("Fetching recently verified payments");

        // Get payments verified in the last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Payment> verifiedPayments = paymentRepository.findByStatusAndConfirmationDateTimeAfter(
                "completed", sevenDaysAgo);

        return verifiedPayments.stream()
                .map(paymentMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get next installment info for an order
     */
    public Map<String, Object> getNextInstallmentInfo(Long orderId) {
        logger.info("Getting next installment info for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        Map<String, Object> result = new HashMap<>();

        // Check if order has an installment plan
        if (order.getInstallmentPlanId() == null) {
            result.put("hasInstallmentPlan", false);
            result.put("message", "No installment plan has been set up for this order");
            return result;
        }

        // Get installment plan details
        try {
            InstallmentPlan plan = installmentPlanRepository.findById(order.getInstallmentPlanId())
                    .orElse(null);

            if (plan == null) {
                result.put("hasInstallmentPlan", false);
                result.put("message", "Installment plan not found");
                return result;
            }

            Integer currentInstallment = order.getCurrentInstallmentNumber();

            result.put("hasInstallmentPlan", true);
            result.put("planId", plan.getId());
            result.put("planName", plan.getName());
            result.put("totalInstallments", plan.getNumberOfInstallments());
            result.put("currentInstallment", currentInstallment);

            // Check if all installments are completed
            if (currentInstallment > plan.getNumberOfInstallments()) {
                result.put("isComplete", true);
                result.put("message", "All installments have been completed");
                return result;
            }

            result.put("isComplete", false);

            // Calculate next installment amount
            Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
            int installmentIndex = currentInstallment - 1;

            if (installmentIndex >= 0 && installmentIndex < plan.getPercentages().size()) {
                Double percentage = plan.getPercentages().get(installmentIndex);
                Double amount = (percentage / 100.0) * totalPrice;
                result.put("nextInstallmentAmount", amount);
            }

            // Get due date if set
            if (order.getNextInstallmentDueDate() != null) {
                result.put("dueDate", order.getNextInstallmentDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                // Calculate days remaining
                LocalDateTime now = LocalDateTime.now();
                long daysRemaining = ChronoUnit.DAYS.between(now, order.getNextInstallmentDueDate());
                result.put("daysRemaining", daysRemaining);

                // Flag if due date is soon (less than 3 days)
                result.put("isDueSoon", daysRemaining <= 3);
                result.put("isOverdue", daysRemaining < 0);
            } else {
                // Calculate a default due date if not set
                setNextInstallmentDueDate(order);
                if (order.getNextInstallmentDueDate() != null) {
                    result.put("suggestedDueDate",
                            order.getNextInstallmentDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
            }

            // Get available payment methods
            result.put("availablePaymentMethods", List.of("payhere", "bank-transfer"));

        } catch (Exception e) {
            logger.error("Error getting next installment info: {}", e.getMessage(), e);
            result.put("error", "Failed to get installment information: " + e.getMessage());
        }

        return result;
    }

    /**
     * Initiate a PayHere payment for an order
     * This method prepares parameters for the PayHere payment gateway
     * CHANGE: Now we don't create a payment record until after successful payment
     */
    @Transactional
    public Map<String, String> initiatePayHerePayment(PaymentRequest paymentRequest) {
        logger.info("Initiating PayHere payment for order ID: {}", paymentRequest.getOrderId());

        // Find the order
        Long orderId = Long.valueOf(paymentRequest.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with id: {}", orderId);
                    return new RuntimeException("Order not found with id: " + orderId);
                });

        // Verify that partial payments are only allowed for events > 24 hours away
        if (paymentRequest.getAmount() < order.getTotalPrice() && isEventWithin24Hours(order)) {
            logger.warn("Attempted partial payment for order with event within 24 hours: {}", orderId);
            throw new RuntimeException("Full payment is required for events within 24 hours");
        }

        // Create session parameters with order and payment info, but don't save a payment record
        Map<String, String> paymentInfo = new HashMap<>();
        paymentInfo.put("orderId", orderId.toString());
        paymentInfo.put("amount", paymentRequest.getAmount().toString());

        if (paymentRequest.getInstallmentPlanId() != null) {
            paymentInfo.put("installmentPlanId", paymentRequest.getInstallmentPlanId().toString());
        }

        if (paymentRequest.getInstallmentNumber() != null) {
            paymentInfo.put("installmentNumber", paymentRequest.getInstallmentNumber().toString());
        }

        if (paymentRequest.getNotes() != null) {
            paymentInfo.put("notes", paymentRequest.getNotes());
        }

        // Build PayHere parameters
        return buildPayHereParameters(order, paymentInfo);
    }

    /**
     * Verify a PayHere payment using the payment ID from PayHere
     * CHANGE: Now we create the payment record on successful verification
     */
    @Transactional
    public OrderResponse verifyPayHerePayment(String orderId, String paymentId) {
        logger.info("Verifying PayHere payment - Order: {}, PayHere Payment ID: {}", orderId, paymentId);

        // Find the order
        Long orderIdLong = Long.valueOf(orderId);
        Order order = orderRepository.findById(orderIdLong)
                .orElseThrow(() -> {
                    logger.error("Order not found with id: {}", orderId);
                    return new RuntimeException("Order not found with id: " + orderId);
                });

        // Create a new payment record for this PayHere transaction
        Payment payment = new Payment();
        payment.setMethod("payhere");
        payment.setStatus("completed");
        payment.setTransactionId(paymentId);
        payment.setConfirmationDateTime(LocalDateTime.now());
        payment.setOrder(order);

        // Set payment amount
        Double totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : 0.0;
        Double totalPaid = order.getPayments().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        // Get payment amount from session or use the remaining amount
        Double amount = Math.min(totalPrice - totalPaid, totalPrice);
        payment.setAmount(amount);

        // Set installment info if available from session
        // These would need to come from the client side when verifying the payment

        // Deactivate all existing payments for this order
        deactivateAllPaymentsForOrder(order);

        // Mark this payment as active
        payment.setIsActive(true);

        // Calculate payment amounts and determine if partial
        calculatePaymentAmounts(payment, order);

        // Add payment to order
        order.getPayments().add(payment);
        payment = paymentRepository.save(payment);
        logger.info("Created completed PayHere payment record with ID: {}", payment.getId());

        // Update order payment status
        updateOrderPaymentStatus(order);

        // Update event calendar
        updateEventAfterPayment(order);

        // Save order with updated status
        Order savedOrder = orderRepository.save(order);

        // Notify customer about payment verification
        try {
            notificationService.createPaymentVerificationNotification(savedOrder, payment, true);
            logger.info("PayHere payment verification notification sent to customer");
        } catch (Exception e) {
            logger.error("Error creating PayHere verification notification: {}", e.getMessage(), e);
        }

        return new OrderResponse(); // Convert order to OrderResponse using mapper
    }

    /**
     * Helper method to deactivate all payments for an order
     */
    private void deactivateAllPaymentsForOrder(Order order) {
        for (Payment existingPayment : order.getPayments()) {
            if (existingPayment.getIsActive() != null && existingPayment.getIsActive()) {
                existingPayment.setIsActive(false);
                paymentRepository.save(existingPayment);
            }
        }
    }

    /**
     * Verify a manual payment (admin)
     * CHANGE: Implement is_active flag properly
     */
    @Transactional
    public OrderResponse verifyManualPayment(Long orderId, Long paymentId, boolean isApproved, String reason) {
        logger.info("Verifying manual payment - Order: {}, Payment: {}, Approved: {}",
                orderId, paymentId, isApproved);

        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with id: {}", orderId);
                    return new RuntimeException("Order not found with id: " + orderId);
                });

        // Find the payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    logger.error("Payment not found with id: {}", paymentId);
                    return new RuntimeException("Payment not found with id: " + paymentId);
                });

        if (isApproved) {
            // Deactivate all existing payments for this order
            deactivateAllPaymentsForOrder(order);

            // Approve payment and set as active
            payment.setStatus("completed");
            payment.setConfirmationDateTime(LocalDateTime.now());
            payment.setIsActive(true);
            logger.info("Payment approved: {}", payment.getId());

            // Update order payment status
            updateOrderPaymentStatus(order);

            // Update event in calendar
            updateEventAfterPayment(order);
        } else {
            // Reject payment
            payment.setStatus("rejected");
            payment.setIsActive(false);
            if (reason != null && !reason.isEmpty()) {
                payment.setRejectionReason(reason);
            }
            logger.info("Payment rejected: {}, reason: {}", payment.getId(), reason);
        }

        // Save payment and order
        paymentRepository.save(payment);
        Order savedOrder = orderRepository.save(order);

        // Notify customer about payment verification
        try {
            notificationService.createPaymentVerificationNotification(savedOrder, payment, isApproved);
            logger.info("Payment verification notification sent to customer");
        } catch (Exception e) {
            logger.error("Error creating verification notification: {}", e.getMessage(), e);
        }

        return new OrderResponse(); // Convert order to OrderResponse using mapper
    }



    /**
     * Build parameters for PayHere payment gateway
     * This overloaded method accepts a Map of payment info instead of a Payment entity
     */
    private Map<String, String> buildPayHereParameters(Order order, Map<String, String> paymentInfo) {
        logger.info("Building PayHere parameters for order ID: {}", order.getId());

        Map<String, String> params = new HashMap<>();

        // Get PayHere configuration from the autowired instance
        String merchantId = payHereConfig.getMerchantId();
        String returnUrl = payHereConfig.getReturnUrl();
        String cancelUrl = payHereConfig.getCancelUrl();
        String notifyUrl = payHereConfig.getNotifyUrl();

        // Customer details
        String customerName = "";
        String customerEmail = "";
        String customerPhone = "";
        String customerAddress = "";

        if (order.getCustomer() != null) {
            customerName = order.getCustomer().getFirstName() != null ?
                    order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName() : "Customer";
            customerEmail = order.getCustomer().getEmail() != null ?
                    order.getCustomer().getEmail() : "";
            customerPhone = order.getCustomer().getContact() != null ?
                    order.getCustomer().getContact() : "";

            // Use a blank address as Customer entity might not have address fields
            customerAddress = "";
        }

        // Item details
        String itemName = "Order #" + order.getId();
        if (order.getEventDetails() != null && order.getEventDetails().getEventCategory() != null) {
            itemName = order.getEventDetails().getEventCategory() + " - Order #" + order.getId();
        }

        // Add required parameters
        params.put("merchant_id", merchantId);
        params.put("return_url", returnUrl);
        params.put("cancel_url", cancelUrl);
        params.put("notify_url", notifyUrl);

        params.put("order_id", order.getId().toString());
        params.put("items", itemName);
        params.put("currency", "LKR"); // Assuming Sri Lankan Rupees
        params.put("amount", paymentInfo.get("amount")); // Use amount from payment info

        // Customer details
        params.put("first_name", customerName);
        params.put("email", customerEmail);
        params.put("phone", customerPhone);
        params.put("address", customerAddress);
        params.put("city", "");
        params.put("country", "Sri Lanka");

        // Custom parameters to track installment info if provided
        if (paymentInfo.containsKey("installmentPlanId")) {
            params.put("custom_1", paymentInfo.get("installmentPlanId"));
        }

        if (paymentInfo.containsKey("installmentNumber")) {
            params.put("custom_2", "installment:" + paymentInfo.get("installmentNumber"));
        }

        // Calculate hash using PayHere verifier
        try {
            String hash = payHereVerifier.generateHash(params);
            params.put("hash", hash);
            logger.debug("Generated PayHere hash: {}", hash);
        } catch (Exception e) {
            logger.error("Error generating PayHere hash: {}", e.getMessage(), e);
        }

        // Add base URL for frontend to construct complete URL
        params.put("payhere_url", payHereConfig.getBaseUrl());

        logger.info("PayHere parameters built successfully");
        return params;
    }

    /**
     * Initialize default installment plans if they don't exist
     * This ensures the database always has the required plans
     */
    @Transactional
    public void initializeDefaultPlans() {
        if (installmentPlanRepository.count() > 0) {
            logger.info("Installment plans already exist - skipping initialization");
            return;
        }

        logger.info("Creating default installment plans");

        // Create and save default plans from InstallmentPlanService
        // This ensures consistency between frontend and backend
        InstallmentPlanService planService = new InstallmentPlanService();
        planService.initializeDefaultPlans();
    }

    /**
     * Process a payment for an order
     * Handles both PayHere and bank transfer methods
     */
    @Transactional
    public OrderResponse processPayment(PaymentRequest paymentRequest) {
        logger.info("Processing payment for order ID: {}", paymentRequest.getOrderId());

        // Find the order
        Long orderId = Long.valueOf(paymentRequest.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with id: {}", orderId);
                    return new RuntimeException("Order not found with id: " + orderId);
                });

        // Verify that partial payments are only allowed for events > 24 hours away
        if (paymentRequest.getAmount() < order.getTotalPrice() && isEventWithin24Hours(order)) {
            logger.warn("Attempted partial payment for order with event within 24 hours: {}", orderId);
            throw new RuntimeException("Full payment is required for events within 24 hours");
        }

        // Create and save payment record
        Payment payment = new Payment();
        payment.setAmount(paymentRequest.getAmount());
        payment.setMethod(paymentRequest.getPaymentMethod());
        payment.setTransactionId(paymentRequest.getTransactionId());
        payment.setStatus("completed");
        payment.setOrder(order);
        payment.setConfirmationDateTime(LocalDateTime.now());

        // Handle installment tracking
        setInstallmentInfo(payment, order, paymentRequest);

        // Calculate payment amounts and determine if partial
        calculatePaymentAmounts(payment, order);

        // Add payment to order's payment list
        order.getPayments().add(payment);
        payment = paymentRepository.save(payment);
        logger.info("Created payment record with ID: {}", payment.getId());

        // Update order payment status
        updateOrderPaymentStatus(order);

        // Update event calendar
        updateEventAfterPayment(order);

        // Save order with updated status
        Order savedOrder = orderRepository.save(order);

        // Create notification about the payment
        try {
            notificationService.createPaymentNotification(savedOrder, payment);
            logger.info("Payment notification created for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Error creating payment notification: {}", e.getMessage(), e);
            // Continue processing as this is non-critical
        }

        return new OrderResponse(); // Convert order to OrderResponse using mapper
    }

}