package com.example.sdpbackend.service;

import com.example.sdpbackend.config.PayHereConfig;
import com.example.sdpbackend.dto.*;
import com.example.sdpbackend.entity.*;
import com.example.sdpbackend.repository.*;
import com.example.sdpbackend.util.PayHereVerifier;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InstallmentPlanService installmentPlanService;

    @Autowired
    private PayHereVerifier payHereVerifier;

    // Payment processing constants
    private static final long PAYMENT_DEADLINE_HOURS = 24; // 24 hours before event
    private static final long MINIMUM_DAYS_FOR_INSTALLMENTS = 7; // 7 days minimum
    private static final double AMOUNT_TOLERANCE = 5.0; // 5% tolerance for amount matching

    /**
     * Process payment - no installment records created until actual payment
     */
    public OrderResponse processPayment(PaymentRequest paymentRequest) {
        logger.info("Processing payment: orderId={}, amount={}, method={}, installmentPlan={}",
                paymentRequest.getOrderId(), paymentRequest.getAmount(), paymentRequest.getPaymentMethod(),
                paymentRequest.getInstallmentPlanId());

        try {
            // Validate and get order
            Order order = validateOrderForPayment(Long.valueOf(paymentRequest.getOrderId()));

            // Validate payment deadline (24 hours before event)
            validatePaymentDeadline(order);

            // Handle PayHere and Bank Transfer differently
            if ("payhere".equals(paymentRequest.getPaymentMethod())) {
                return processPayHereInitiation(order, paymentRequest);
            } else {
                return processBankTransferInitiation(order, paymentRequest);
            }
        } catch (Exception e) {
            logger.error("Error processing payment for order {}: {}", paymentRequest.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * PayHere initiation - only generate parameters, no DB records
     */
    private OrderResponse processPayHereInitiation(Order order, PaymentRequest request) {
        logger.info("Initiating PayHere payment for order: {}", order.getId());

        // Validate installment plan availability
        if (request.getInstallmentPlanId() != null && request.getInstallmentPlanId() > 1) {
            validateInstallmentPlanAvailability(order, request.getInstallmentPlanId());
        }

        // Generate PayHere parameters without creating ANY DB records
        Map<String, String> payHereParams = generatePayHereParams(order, request);

        OrderResponse response = createOrderResponse(order);
        response.setPayHereParams(payHereParams);

        logger.info("Generated PayHere parameters for order: {} (NO DB records created)", order.getId());
        return response;
    }

    /**
     * Bank transfer initiation - create payment record but NO installment records
     */
    public OrderResponse processBankTransferInitiation(Order order, PaymentRequest request) {
        logger.info("Initiating bank transfer payment for order: {}", order.getId());

        // Create or get payment record WITHOUT installment records
        Payment payment = getOrCreatePaymentRecordWithoutInstallments(order, request);

        // Update order status to indicate payment process started
        order.setPaymentStatus("pending-upload");
        orderRepository.save(order);

        logger.info("Bank transfer payment initiated for order: {} with payment ID: {} (NO installment records created yet)",
                order.getId(), payment.getId());
        return createOrderResponse(order);
    }

    /**
     * Create payment record WITHOUT any installment records
     */
    public Payment getOrCreatePaymentRecordWithoutInstallments(Order order, PaymentRequest request) {
        // Check for existing active payment
        Payment existingPayment = getActivePaymentForOrder(order);

        if (existingPayment != null) {
            logger.info("Using existing payment record: {} (no new installment records)",
                    existingPayment.getId());
            return existingPayment;
        }

        // Create new payment record WITHOUT installment records
        return createNewPaymentRecordWithoutInstallments(order, request);
    }

    /**
     * Validate order is eligible for payment
     */
    private Order validateOrderForPayment(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (!"confirmed".equals(order.getStatus()) && !"partial-payment".equals(order.getStatus())) {
            throw new RuntimeException("Order must be confirmed before payment can be processed");
        }

        return order;
    }

    /**
     * Validate payment deadline (24 hours before event)
     */
    private void validatePaymentDeadline(Order order) {
        if (order.getEventDetails() == null || order.getEventDetails().getEventDate() == null) {
            logger.warn("Order {} has no event date, skipping deadline validation", order.getId());
            return;
        }

        try {
            LocalDateTime eventDateTime = parseEventDateTime(order);
            LocalDateTime deadline = eventDateTime.minusHours(PAYMENT_DEADLINE_HOURS);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(deadline)) {
                throw new RuntimeException("Payment deadline has passed. Payments must be completed " +
                        PAYMENT_DEADLINE_HOURS + " hours before the event.");
            }

            logger.info("Payment deadline validation passed. Deadline: {}, Current time: {}", deadline, now);
        } catch (Exception e) {
            logger.error("Error validating payment deadline for order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Error validating payment deadline: " + e.getMessage());
        }
    }

    /**
     * Validate installment plan availability based on installment plan ID
     */
    private void validateInstallmentPlanAvailability(Order order, Integer installmentPlanId) {
        if (installmentPlanId == null || installmentPlanId <= 0) {
            throw new IllegalArgumentException("Invalid installment plan ID");
        }

        InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(installmentPlanId.longValue());
        if (plan == null) {
            throw new RuntimeException("Installment plan not found with ID: " + installmentPlanId);
        }

        LocalDateTime eventDateTime = parseEventDateTime(order);
        LocalDateTime now = LocalDateTime.now();
        long daysUntilEvent = ChronoUnit.DAYS.between(now.toLocalDate(), eventDateTime.toLocalDate());
        long weeksUntilEvent = daysUntilEvent / 7;

        logger.info("Validating installment plan: {} installments, {} days until event, {} weeks",
                plan.getNumberOfInstallments(), daysUntilEvent, weeksUntilEvent);

        // Validate based on requirements
        switch (plan.getNumberOfInstallments()) {
            case 1: // Full payment - always available
                break;
            case 2: // 50% split - requires 2+ weeks
                if (weeksUntilEvent < 2) {
                    throw new RuntimeException("50% installment plan requires at least 2 weeks until event. You have " + weeksUntilEvent + " weeks.");
                }
                break;
            case 3: // 33.3% split - requires 3+ weeks
                if (weeksUntilEvent < 3) {
                    throw new RuntimeException("33.3% installment plan requires at least 3 weeks until event. You have " + weeksUntilEvent + " weeks.");
                }
                break;
            case 4: // 25% split - requires 4+ weeks
                if (weeksUntilEvent < 4) {
                    throw new RuntimeException("25% split requires at least 4 weeks until event. You have " + weeksUntilEvent + " weeks.");
                }
                break;
            default:
                throw new RuntimeException("Unsupported installment plan: " + plan.getNumberOfInstallments() + " installments");
        }

        // Additional check for minimum days
        if (daysUntilEvent < MINIMUM_DAYS_FOR_INSTALLMENTS && plan.getNumberOfInstallments() > 1) {
            throw new RuntimeException("Installment payments are only available for events more than " +
                    MINIMUM_DAYS_FOR_INSTALLMENTS + " days away. Your event is in " + daysUntilEvent + " days.");
        }
    }

    /**
     * Parse event date and time into LocalDateTime
     */
    private LocalDateTime parseEventDateTime(Order order) {
        try {
            String eventDateStr = order.getEventDetails().getEventDate();
            LocalDateTime eventDateTime;

            if (eventDateStr.contains("T")) {
                eventDateTime = LocalDateTime.parse(eventDateStr);
            } else {
                eventDateTime = LocalDate.parse(eventDateStr).atStartOfDay();
            }

            // Add event time if available
            if (order.getEventDetails().getEventTime() != null) {
                String[] timeParts = order.getEventDetails().getEventTime().split(":");
                int hours = Integer.parseInt(timeParts[0]);
                int minutes = Integer.parseInt(timeParts[1]);
                eventDateTime = eventDateTime.withHour(hours).withMinute(minutes);
            }

            return eventDateTime;
        } catch (Exception e) {
            logger.error("Error parsing event date/time for order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Invalid event date/time format");
        }
    }

    /**
     * Get existing payment or create new payment record (only for bank transfers)
     */
    private Payment getOrCreatePaymentRecord(Order order, PaymentRequest request) {
        // Check for existing active payment
        Payment existingPayment = getActivePaymentForOrder(order);

        if (existingPayment != null) {
            logger.info("Using existing payment record: {}", existingPayment.getId());
            return existingPayment;
        }

        // Create new payment record - fixed method name
        return createNewPaymentRecordWithoutInstallments(order, request);
    }

    /**
     * Create payment record WITHOUT creating any installment records
     */
    public Payment createNewPaymentRecordWithoutInstallments(Order order, PaymentRequest request) {
        if (order == null || request == null) {
            throw new IllegalArgumentException("Order and request cannot be null");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTotalAmount(order.getTotalPrice());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setNotes(request.getNotes());

        // Set installment plan details but don't create installment records
        if (request.getInstallmentPlanId() != null && request.getInstallmentPlanId() > 1) {
            // Installment payment setup
            InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(request.getInstallmentPlanId().longValue());

            // Validate installment plan availability
            validateInstallmentPlanAvailability(order, request.getInstallmentPlanId());

            payment.setInstallmentPlanId(plan.getId());
            payment.setPaymentType("installment");
            payment.setCurrentInstallment(1);
            payment.setTotalInstallments(plan.getNumberOfInstallments());
            payment.setAmount(calculateInstallmentAmount(plan, 1, order.getTotalPrice()));

            logger.info("Created installment payment setup with plan ID: {} ({}), installments: {} - NO installment records created",
                    plan.getId(), plan.getName(), plan.getNumberOfInstallments());
        } else {
            // Full payment setup
            payment.setInstallmentPlanId(1L);
            payment.setPaymentType("full");
            payment.setCurrentInstallment(1);
            payment.setTotalInstallments(1);
            payment.setAmount(order.getTotalPrice());

            logger.info("Created full payment setup - NO installment records created");
        }

        payment.setStatus("pending");

        // Save payment record only - NO installment records
        payment = paymentRepository.save(payment);

        logger.info("Payment record created successfully with ID: {} - NO installment records created yet",
                payment.getId());

        return payment;
    }

    /**
     * Create only the current installment instead of all installments
     */
    public void createCurrentInstallmentOnly(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }

        try {
            Integer currentInstallmentNumber = getCurrentInstallmentNumber(payment);

            if ("full".equals(payment.getPaymentType())) {
                // Create single installment for full payment
                createSingleInstallmentWithValidation(payment, 1, payment.getAmount(), 100.0);
                logger.info("Created single installment record for full payment");
            } else {
                // Create ONLY the current installment for installment payments
                InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(payment.getInstallmentPlanId());

                Double percentage = plan.getPercentages().get(currentInstallmentNumber - 1);
                Double amount = calculateInstallmentAmount(plan, currentInstallmentNumber, payment.getTotalAmount());

                createSingleInstallmentWithValidation(payment, currentInstallmentNumber, amount, percentage);

                logger.info("Created ONLY current installment {} for installment payment (not all {} installments)",
                        currentInstallmentNumber, plan.getNumberOfInstallments());
            }
        } catch (Exception e) {
            logger.error("Failed to create current installment for payment {}: {}",
                    payment.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create current installment: " + e.getMessage());
        }
    }



    private Integer getCurrentInstallmentNumber(Payment payment) {
        if (payment == null) {
            return 1;
        }
        // Use the currentInstallment field directly from Payment entity
        Integer current = payment.getCurrentInstallmentNumber();
        return current != null ? current : 1;
    }

    /**
     * Calculate installment amount based on plan and installment number
     */
    private Double calculateInstallmentAmount(InstallmentPlan plan, Integer installmentNumber, Double totalAmount) {
        if (plan == null || installmentNumber == null || totalAmount == null) {
            throw new IllegalArgumentException("Plan, installment number, and total amount cannot be null");
        }

        if (installmentNumber < 1 || installmentNumber > plan.getNumberOfInstallments()) {
            throw new RuntimeException("Invalid installment number: " + installmentNumber);
        }

        Double percentage = plan.getPercentages().get(installmentNumber - 1);
        return (percentage / 100.0) * totalAmount;
    }


    /**
     * Create single installment with validation
     */
    private void createSingleInstallmentWithValidation(Payment payment, Integer installmentNumber,
                                                       Double amount, Double percentage) {
        // Validate input parameters
        if (payment == null || installmentNumber == null || installmentNumber < 1 ||
                amount == null || amount <= 0 || percentage == null || percentage <= 0) {
            throw new RuntimeException("Invalid parameters for installment creation");
        }

        // Check if installment already exists
        Installment existingInstallment = installmentRepository.findByPaymentIdAndInstallmentNumber(
                payment.getId(), installmentNumber);

        if (existingInstallment != null) {
            logger.warn("Installment {} already exists for payment {} - skipping creation",
                    installmentNumber, payment.getId());
            return;
        }

        Installment installment = new Installment();
        installment.setPayment(payment);
        installment.setInstallmentNumber(installmentNumber);
        installment.setAmount(amount);
        installment.setPercentage(percentage);
        installment.setStatus("pending");
        installment.setPaymentMethod(payment.getPaymentMethod());

        installmentRepository.save(installment);

        logger.info("Created installment {}/{} with amount {} for payment {}",
                installmentNumber, payment.getTotalInstallments(), amount, payment.getId());
    }

    /**
     * Generate PayHere payment parameters
     */
    private Map<String, String> generatePayHereParams(Order order, PaymentRequest request) {
        if (order == null || request == null) {
            throw new IllegalArgumentException("Order and request cannot be null");
        }

        Map<String, String> params = new HashMap<>();

        Customer customer = order.getCustomer();

        // Basic PayHere parameters
        params.put("merchant_id", "1221149"); // Your merchant ID
        params.put("return_url", "http://localhost:4200/orders/ongoing");
        params.put("cancel_url", "http://localhost:4200/orders/ongoing");
        params.put("notify_url", "http://localhost:8083/api/orders/payhere/notify");

        // Order details
        params.put("order_id", order.getId().toString());
        params.put("items", getPaymentDescription(order, request));
        params.put("currency", "LKR");
        params.put("amount", String.format("%.2f", request.getAmount()));

        // Customer details
        params.put("first_name", customer.getFirstName());
        params.put("last_name", customer.getLastName());
        params.put("email", customer.getEmail());
        params.put("phone", customer.getContact());
        params.put("address", "");
        params.put("city", "");
        params.put("country", "Sri Lanka");

        // Additional parameters for installment tracking
        if (request.getInstallmentPlanId() != null) {
            params.put("custom_1", request.getInstallmentPlanId().toString());
            params.put("custom_2", request.getInstallmentNumber() != null ? request.getInstallmentNumber().toString() : "1");
        }

        return params;
    }

    /**
     * Get payment description for PayHere
     */
    private String getPaymentDescription(Order order, PaymentRequest request) {
        if (request.getInstallmentPlanId() != null && request.getInstallmentPlanId() > 1) {
            InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(request.getInstallmentPlanId().longValue());
            Integer installmentNumber = request.getInstallmentNumber() != null ? request.getInstallmentNumber() : 1;
            return String.format("Order #%s - %s (Installment %d of %d)",
                    order.getOrderNumber(),
                    plan.getName(),
                    installmentNumber,
                    plan.getNumberOfInstallments());
        } else {
            return String.format("Order #%s - Full Payment", order.getOrderNumber());
        }
    }

    /**
     * MODIFIED METHOD - Verify PayHere payment - creates installment record only on successful verification
     * FIXED: Only create the current installment being paid, not all future installments
     */
    public OrderResponse verifyPayHerePayment(String orderId, String paymentId) {
        logger.info("Verifying PayHere payment: orderId={}, paymentId={}", orderId, paymentId);

        Order order = orderRepository.findById(Long.valueOf(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Get or create payment record
        Payment payment = getActivePaymentForOrder(order);

        if (payment == null) {
            // Create payment record for PayHere success (this should be from PayHere params)
            payment = createPayHerePaymentRecord(order, paymentId);
        }

        // FIXED: Create installment record ONLY for the current installment being paid
        Installment currentInstallment = createInstallmentRecordForPayHereSuccess(payment, paymentId);

        // Check if all installments are completed
        List<Installment> allInstallments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(payment.getId());
        long confirmedCount = allInstallments.stream()
                .filter(i -> "confirmed".equals(i.getStatus()))
                .count();

        if (confirmedCount == payment.getTotalInstallments()) {
            payment.setStatus("completed");
            order.setPaymentStatus("completed");
            logger.info("All installments completed for payment: {}", payment.getId());
        } else {
            // FIXED: Only update current installment number, don't create next installment record
            Integer nextInstallmentNumber = currentInstallment.getInstallmentNumber() + 1;
            if (nextInstallmentNumber <= payment.getTotalInstallments()) {
                payment.setCurrentInstallment(nextInstallmentNumber);
                logger.info("Updated current installment to: {} (no record created yet)", nextInstallmentNumber);
            }
            payment.setStatus("partial");
            order.setPaymentStatus("partial");
        }

        paymentRepository.save(payment);
        orderRepository.save(order);

        logger.info("PayHere payment verification completed successfully");

        return createOrderResponse(order);
    }

    /**
     * Create PayHere payment record only after successful verification
     */
    public Payment createPayHerePaymentRecord(Order order, String paymentId) {
        logger.info("Creating PayHere payment record after successful verification for order: {}", order.getId());

        // For PayHere, we need to determine the installment plan from the query params or create full payment
        // For now, assume full payment unless we have other data
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTotalAmount(order.getTotalPrice());
        payment.setPaymentMethod("payhere");
        payment.setPaymentType("full");
        payment.setCurrentInstallment(1);
        payment.setTotalInstallments(1);
        payment.setAmount(order.getTotalPrice());
        payment.setStatus("confirmed");

        payment = paymentRepository.save(payment);

        // Create installment record
        createSingleInstallmentWithValidation(payment, 1, payment.getAmount(), 100.0);

        return payment;
    }

    /**
     * Create installment record only for successful PayHere payment
     */
    public Installment createInstallmentRecordForPayHereSuccess(Payment payment, String paymentId) {
        Integer currentInstallmentNumber = getCurrentInstallmentNumber(payment);

        // Check if installment record already exists
        Installment existingInstallment = installmentRepository.findByPaymentIdAndInstallmentNumber(
                payment.getId(), currentInstallmentNumber);

        if (existingInstallment != null) {
            // Update existing installment as confirmed
            existingInstallment.setStatus("confirmed");
            existingInstallment.setTransactionId(paymentId);
            existingInstallment.setConfirmationDate(LocalDateTime.now());
            existingInstallment.setVerifiedBy("PayHere Auto-Verification");
            installmentRepository.save(existingInstallment);
            logger.info("Updated existing installment {} as confirmed", currentInstallmentNumber);
            return existingInstallment;
        }

        // Create new installment record for PayHere success
        Installment installment = new Installment();
        installment.setPayment(payment);
        installment.setInstallmentNumber(currentInstallmentNumber);
        installment.setStatus("confirmed"); // Auto-confirmed for PayHere
        installment.setPaymentMethod("payhere");
        installment.setTransactionId(paymentId);
        installment.setConfirmationDate(LocalDateTime.now());
        installment.setVerifiedBy("PayHere Auto-Verification");

        // Calculate amount and percentage for current installment
        if ("full".equals(payment.getPaymentType())) {
            installment.setAmount(payment.getTotalAmount());
            installment.setPercentage(100.0);
        } else {
            InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(payment.getInstallmentPlanId());
            Double percentage = plan.getPercentages().get(currentInstallmentNumber - 1);
            Double amount = calculateInstallmentAmount(plan, currentInstallmentNumber, payment.getTotalAmount());

            installment.setAmount(amount);
            installment.setPercentage(percentage);
        }

        installment = installmentRepository.save(installment);

        logger.info("Created NEW confirmed installment record {} for PayHere payment {}",
                currentInstallmentNumber, payment.getId());

        return installment;
    }

    /**
     * Upload payment slip - creates installment record only when slip is uploaded
     */
    public OrderResponse uploadPaymentSlip(Long orderId, String imageUrl, Double amount,
                                           Boolean isPartialPayment, String notes) {
        logger.info("Uploading payment slip: orderId={}, amount={}", orderId, amount);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        Payment payment = getActivePaymentForOrder(order);

        if (payment == null) {
            // Create new bank transfer payment setup
            logger.info("No active payment found for order {}, creating new bank transfer payment", orderId);
            payment = createBankTransferPaymentFromAmount(order, amount, notes);
        }

        // Create installment record ONLY when payment slip is uploaded
        Installment currentInstallment = createInstallmentRecordForCurrentPayment(payment, imageUrl, notes);

        // Update payment and order status
        updatePaymentStatus(payment);
        order.setPaymentStatus("pending-verification");

        paymentRepository.save(payment);
        orderRepository.save(order);

        logger.info("Payment slip uploaded successfully - Created installment {} for payment {}",
                currentInstallment.getInstallmentNumber(), payment.getId());

        return createOrderResponse(order);
    }

    /**
     * MODIFIED METHOD - Create installment record only for current payment when slip is uploaded
     * FIXED: Only create the specific installment being paid, not any future installments
     */
    public Installment createInstallmentRecordForCurrentPayment(Payment payment, String imageUrl, String notes) {
        Integer currentInstallmentNumber = getCurrentInstallmentNumber(payment);

        // Check if installment record already exists
        Installment existingInstallment = installmentRepository.findByPaymentIdAndInstallmentNumber(
                payment.getId(), currentInstallmentNumber);

        if (existingInstallment != null) {
            // Update existing installment with new slip details
            updateInstallmentSlipDetails(existingInstallment, imageUrl, notes);
            installmentRepository.save(existingInstallment);
            logger.info("Updated existing installment {} with new payment slip", currentInstallmentNumber);
            return existingInstallment;
        }

        // FIXED: Create new installment record ONLY for the current payment being made
        Installment installment = new Installment();
        installment.setPayment(payment);
        installment.setInstallmentNumber(currentInstallmentNumber);
        installment.setStatus("pending");
        installment.setPaymentMethod(payment.getPaymentMethod());

        // Calculate amount and percentage for current installment
        if ("full".equals(payment.getPaymentType())) {
            installment.setAmount(payment.getTotalAmount());
            installment.setPercentage(100.0);
        } else {
            InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(payment.getInstallmentPlanId());
            Double percentage = plan.getPercentages().get(currentInstallmentNumber - 1);
            Double amount = calculateInstallmentAmount(plan, currentInstallmentNumber, payment.getTotalAmount());

            installment.setAmount(amount);
            installment.setPercentage(percentage);
        }

        // Set payment slip details
        updateInstallmentSlipDetails(installment, imageUrl, notes);

        installment = installmentRepository.save(installment);

        logger.info("Created NEW installment record {} for payment {} with slip upload (ONLY current installment)",
                currentInstallmentNumber, payment.getId());

        return installment;
    }

    /**
     * Helper method to update installment slip details
     */
    private void updateInstallmentSlipDetails(Installment installment, String imageUrl, String notes) {
        installment.setPaymentSlipUrl(imageUrl);
        installment.setNotes(notes);
        installment.setPaymentDate(LocalDateTime.now());
    }

    /**
     * Find current installment for payment with better error handling
     */
    private Installment findCurrentInstallmentForPayment(Payment payment) {
        if (payment == null) {
            logger.warn("Payment is null, cannot find current installment");
            return null;
        }

        // Try to get current installment by installment number
        Integer currentInstallmentNumber = getCurrentInstallmentNumber(payment);
        if (currentInstallmentNumber != null) {
            Installment currentInstallment = installmentRepository.findByPaymentIdAndInstallmentNumber(
                    payment.getId(), currentInstallmentNumber);

            if (currentInstallment != null) {
                logger.info("Found current installment {} for payment {}",
                        currentInstallment.getInstallmentNumber(), payment.getId());
                return currentInstallment;
            }
        }

        // If no current installment found, try to find the next pending installment
        List<Installment> installments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(payment.getId());
        List<Installment> pendingInstallments = installments.stream()
                .filter(i -> "pending".equals(i.getStatus()))
                .sorted((i1, i2) -> i1.getInstallmentNumber().compareTo(i2.getInstallmentNumber()))
                .collect(Collectors.toList());

        if (!pendingInstallments.isEmpty()) {
            Installment nextPending = pendingInstallments.get(0);
            logger.info("Found next pending installment {} for payment {}",
                    nextPending.getInstallmentNumber(), payment.getId());
            return nextPending;
        }

        return null;
    }

    /**
     * Create bank transfer payment from amount - no installment records
     */
    public Payment createBankTransferPaymentFromAmount(Order order, Double amount, String notes) {
        logger.info("Creating bank transfer payment for order {} with amount {}", order.getId(), amount);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTotalAmount(order.getTotalPrice());
        payment.setPaymentMethod("bank-transfer");
        payment.setNotes(notes);

        // Intelligent installment plan detection based on amount
        Integer installmentPlanId = determineInstallmentPlanFromAmount(order.getTotalPrice(), amount);

        if (installmentPlanId > 1) {
            // Installment payment setup
            try {
                InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(installmentPlanId.longValue());

                // Validate installment plan availability
                validateInstallmentPlanAvailability(order, installmentPlanId);

                payment.setPaymentType("installment");
                payment.setInstallmentPlanId(installmentPlanId.longValue());
                payment.setCurrentInstallment(1);
                payment.setTotalInstallments(plan.getNumberOfInstallments());
                payment.setAmount(amount);

                logger.info("Created installment payment setup with plan: {} - NO installment records created", plan.getName());
            } catch (Exception e) {
                logger.warn("Failed to create installment payment, falling back to full payment: {}", e.getMessage());
                // Fallback to full payment
                setupFullPayment(payment, amount);
            }
        } else {
            // Full payment setup
            setupFullPayment(payment, amount);
        }

        payment.setStatus("pending");
        payment = paymentRepository.save(payment);

        logger.info("Bank transfer payment created with ID: {} - NO installment records created", payment.getId());

        return payment;
    }

    /**
     * Create fallback installment when none exists
     */
    public Installment createFallbackInstallment(Payment payment, Double amount) {
        logger.warn("Creating fallback installment for payment {} with amount {}",
                payment.getId(), amount);

        if (payment == null) {
            throw new RuntimeException("Cannot create fallback installment: Payment is null");
        }

        // Determine installment number
        Integer installmentNumber = getCurrentInstallmentNumber(payment);
        if (installmentNumber == null || installmentNumber < 1) {
            installmentNumber = 1;
            payment.setCurrentInstallment(installmentNumber);
        }

        // Calculate percentage based on amount
        Double percentage = 100.0;
        if (payment.getTotalAmount() != null && payment.getTotalAmount() > 0) {
            percentage = (amount / payment.getTotalAmount()) * 100.0;
        }

        // Create fallback installment
        Installment fallbackInstallment = new Installment();
        fallbackInstallment.setPayment(payment);
        fallbackInstallment.setInstallmentNumber(installmentNumber);
        fallbackInstallment.setAmount(amount);
        fallbackInstallment.setPercentage(percentage);
        fallbackInstallment.setStatus("pending");
        fallbackInstallment.setPaymentMethod(payment.getPaymentMethod());

        // Save the installment
        fallbackInstallment = installmentRepository.save(fallbackInstallment);

        // Update payment total installments if needed
        if (payment.getTotalInstallments() == null || payment.getTotalInstallments() < installmentNumber) {
            payment.setTotalInstallments(installmentNumber);
            paymentRepository.save(payment);
        }

        logger.info("Created fallback installment {} for payment {} with amount {}",
                fallbackInstallment.getId(), payment.getId(), amount);

        return fallbackInstallment;
    }

    /**
     * Setup full payment with proper validation
     */
    private void setupFullPayment(Payment payment, Double amount) {
        payment.setPaymentType("full");
        payment.setCurrentInstallment(1);
        payment.setTotalInstallments(1);
        payment.setAmount(Math.min(amount, payment.getTotalAmount())); // Cap at total amount

        logger.info("Setup full payment with amount: {}", payment.getAmount());
    }

    /**
     * Intelligent installment plan detection based on amount percentage
     */
    private Integer determineInstallmentPlanFromAmount(Double totalPrice, Double amount) {
        if (totalPrice == null || amount == null || amount <= 0) {
            return 1; // Full payment
        }

        double percentage = (amount / totalPrice) * 100;

        // Tolerance for rounding
        double tolerance = AMOUNT_TOLERANCE;

        if (Math.abs(percentage - 50.0) <= tolerance) {
            return 2; // 50% split
        } else if (Math.abs(percentage - 33.33) <= tolerance) {
            return 3; // 33.3% split
        } else if (Math.abs(percentage - 25.0) <= tolerance) {
            return 4; // 25% split
        } else if (percentage >= 95.0) {
            return 1; // Full payment (allowing for small differences)
        } else {
            // For any other percentage, default to 50% split if amount is less than 75% of total
            if (percentage < 75.0) {
                logger.info("Amount {}% of total, defaulting to 50% installment plan", percentage);
                return 2;
            } else {
                return 1; // Full payment
            }
        }
    }

    /**
     * Verify manual payment - only affects existing installment records
     */
    public OrderResponse verifyManualPayment(Long orderId, Long paymentId, boolean isApproved, String reason) {
        logger.info("Verifying manual payment: orderId={}, paymentId={}, approved={}", orderId, paymentId, isApproved);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        // Find current installment record (should exist from slip upload)
        Installment currentInstallment = installmentRepository.findByPaymentIdAndInstallmentNumber(
                payment.getId(), getCurrentInstallmentNumber(payment));

        if (currentInstallment == null) {
            throw new RuntimeException("No installment record found for verification. Payment slip must be uploaded first.");
        }

        // Get current user for verification tracking
        String verifierName = getCurrentVerifierName();

        if (isApproved) {
            // Approve the installment
            String transactionRef = generateTransactionReference(payment, currentInstallment);
            confirmInstallmentPayment(currentInstallment, verifierName, transactionRef);

            logger.info("Approved installment {} for payment {} by {}",
                    currentInstallment.getInstallmentNumber(), payment.getId(), verifierName);

            // Move to next installment if not the last one
            if (getCurrentInstallmentNumber(payment) < payment.getTotalInstallments()) {
                payment.setCurrentInstallment(getCurrentInstallmentNumber(payment) + 1);
                payment.setStatus("partial");
                order.setPaymentStatus("partial");
                logger.info("Moved to next installment: {}", getCurrentInstallmentNumber(payment));
            } else {
                // All installments completed
                payment.setStatus("completed");
                order.setPaymentStatus("completed");
                logger.info("All installments completed for payment: {}", payment.getId());
            }

        } else {
            // Reject the installment
            rejectInstallmentPayment(currentInstallment, verifierName, reason);
            payment.setStatus("rejected");
            order.setPaymentStatus("rejected");

            logger.info("Rejected installment {} with reason: {} by {}",
                    currentInstallment.getInstallmentNumber(), reason, verifierName);
        }

        // Save all changes
        installmentRepository.save(currentInstallment);
        paymentRepository.save(payment);
        orderRepository.save(order);

        logger.info("Payment verification completed successfully for payment: {}", payment.getId());
        return createOrderResponse(order);
    }

    /**
     * Helper method to confirm installment payment
     */
    private void confirmInstallmentPayment(Installment installment, String verifierName, String transactionRef) {
        installment.setStatus("confirmed");
        installment.setVerifiedBy(verifierName);
        installment.setTransactionId(transactionRef);
        installment.setConfirmationDate(LocalDateTime.now());
    }

    /**
     * Helper method to reject installment payment
     */
    private void rejectInstallmentPayment(Installment installment, String verifierName, String reason) {
        installment.setStatus("rejected");
        installment.setVerifiedBy(verifierName);
        installment.setRejectionReason(reason);
        installment.setConfirmationDate(LocalDateTime.now());
    }

    /**
     * Find current installment with proper validation
     */
    private Installment findCurrentInstallmentWithValidation(Payment payment) {
        if (payment == null || getCurrentInstallmentNumber(payment) == null) {
            logger.error("Invalid payment or current installment number is null");
            return null;
        }

        // First try to find by current installment number using repository method
        Installment currentInstallment = installmentRepository.findByPaymentIdAndInstallmentNumber(
                payment.getId(), getCurrentInstallmentNumber(payment));

        if (currentInstallment != null) {
            logger.debug("Found current installment {} for payment {}",
                    currentInstallment.getInstallmentNumber(), payment.getId());
            return currentInstallment;
        }

        // If not found, find the first pending installment
        List<Installment> installments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(payment.getId());
        currentInstallment = installments.stream()
                .filter(i -> "pending".equals(i.getStatus()))
                .min((i1, i2) -> i1.getInstallmentNumber().compareTo(i2.getInstallmentNumber()))
                .orElse(null);

        if (currentInstallment != null) {
            logger.warn("Current installment number mismatch for payment {}, using first pending: {}",
                    payment.getId(), currentInstallment.getInstallmentNumber());
            return currentInstallment;
        }

        logger.error("No current installment found for payment {}", payment.getId());
        return null;
    }

    /**
     * Get current verifier name from security context
     */
    private String getCurrentVerifierName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn("Could not get current user name: {}", e.getMessage());
        }
        return "System Admin"; // Fallback
    }

    /**
     * Generate transaction reference for manual payments
     */
    private String generateTransactionReference(Payment payment, Installment installment) {
        return String.format("MAN-%d-%d-%d",
                payment.getOrder().getId(),
                payment.getId(),
                installment.getInstallmentNumber());
    }

    /**
     * Update payment status based on installments
     */
    private void updatePaymentStatus(Payment payment) {
        if (payment == null) {
            return;
        }

        List<Installment> installments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(payment.getId());

        if (installments.isEmpty()) {
            payment.setStatus("pending");
            return;
        }

        long confirmedCount = installments.stream()
                .filter(i -> "confirmed".equals(i.getStatus()))
                .count();

        if (confirmedCount == payment.getTotalInstallments()) {
            payment.setStatus("completed");
        } else if (confirmedCount > 0) {
            payment.setStatus("partial");
        } else {
            payment.setStatus("pending");
        }
    }

    /**
     * Check if all installments are completed for a payment
     */
    private boolean areAllInstallmentsCompleted(Payment payment) {
        List<Installment> installments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(payment.getId());

        long confirmedCount = installments.stream()
                .filter(i -> "confirmed".equals(i.getStatus()))
                .count();

        boolean allCompleted = confirmedCount == payment.getTotalInstallments();
        logger.info("Payment {} installment status: {}/{} confirmed",
                payment.getId(), confirmedCount, payment.getTotalInstallments());

        return allCompleted;
    }

    /**
     * Get active payment for an order
     */
    public Payment getActivePaymentForOrder(Order order) {
        if (order == null || order.getPayments() == null) {
            return null;
        }

        return order.getPayments().stream()
                .filter(p -> !"rejected".equals(p.getStatus()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get available installment plans for an order based on timing
     */
    public List<InstallmentPlanDTO> getAvailableInstallmentPlansForOrder(Long orderId) {
        logger.info("Getting available installment plans for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        LocalDateTime eventDateTime = parseEventDateTime(order);
        LocalDateTime now = LocalDateTime.now();
        long daysUntilEvent = ChronoUnit.DAYS.between(now.toLocalDate(), eventDateTime.toLocalDate());
        long weeksUntilEvent = daysUntilEvent / 7;

        logger.info("Order timing: {} days until event, {} weeks", daysUntilEvent, weeksUntilEvent);

        List<InstallmentPlan> allPlans = installmentPlanService.getAllActivePlans();
        List<InstallmentPlanDTO> availablePlans = new ArrayList<>();

        for (InstallmentPlan plan : allPlans) {
            InstallmentPlanDTO planDTO = convertToDTO(plan);

            // Check if plan is available based on timing constraints
            boolean isAvailable = isInstallmentPlanAvailable(plan, daysUntilEvent, weeksUntilEvent);

            if (isAvailable) {
                // Set order-specific information
                setTimeRequirementBasedOnInstallments(planDTO);

                // Generate breakdown for this order
                generateBreakdown(planDTO, order.getTotalPrice());

                availablePlans.add(planDTO);
                logger.info("Plan available: {} ({} installments)", plan.getName(), plan.getNumberOfInstallments());
            } else {
                logger.info("Plan not available: {} ({} installments) - insufficient time",
                        plan.getName(), plan.getNumberOfInstallments());
            }
        }

        logger.info("Total available plans: {}", availablePlans.size());
        return availablePlans;
    }

    /**
     * Check if installment plan is available based on timing
     */
    private boolean isInstallmentPlanAvailable(InstallmentPlan plan, long daysUntilEvent, long weeksUntilEvent) {
        // Updated logic: 7 days minimum for any installment plan
        if (daysUntilEvent < MINIMUM_DAYS_FOR_INSTALLMENTS && plan.getNumberOfInstallments() > 1) {
            return false;
        }

        switch (plan.getNumberOfInstallments()) {
            case 1: // Full payment - always available
                return true;
            case 2: // 50% split - requires 2+ weeks
                return weeksUntilEvent >= 2;
            case 3: // 33.3% split - requires 3+ weeks
                return weeksUntilEvent >= 3;
            case 4: // 25% split - requires 4+ weeks
                return weeksUntilEvent >= 4;
            default:
                return false;
        }
    }

    /**
     * Convert InstallmentPlan entity to DTO
     */
    private InstallmentPlanDTO convertToDTO(InstallmentPlan plan) {
        InstallmentPlanDTO dto = new InstallmentPlanDTO();
        dto.setId(plan.getId().intValue());
        dto.setName(plan.getName());
        dto.setNumberOfInstallments(plan.getNumberOfInstallments());
        dto.setPercentages(new ArrayList<>(plan.getPercentages()));
        dto.setDescription(plan.getDescription());
        dto.setIsActive(plan.getIsActive());
        return dto;
    }

    /**
     * Set time requirement based on installments for DTO
     */
    private void setTimeRequirementBasedOnInstallments(InstallmentPlanDTO planDTO) {
        // This method would set time requirements based on the number of installments
        // Implementation depends on your DTO structure
    }

    /**
     * Generate breakdown for installment plan DTO
     */
    private void generateBreakdown(InstallmentPlanDTO planDTO, Double totalPrice) {
        // This method would generate the breakdown based on total price
        // Implementation depends on your DTO structure
    }

    /**
     * Get payment summary for an order
     */
    public PaymentSummaryDTO getPaymentSummary(Long orderId) {
        logger.info("Getting payment summary for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        PaymentSummaryDTO summary = new PaymentSummaryDTO();
        summary.setTotalAmount(order.getTotalPrice());

        Payment activePayment = getActivePaymentForOrder(order);

        if (activePayment != null) {
            // Calculate payment summary from installments
            summary.setTotalPaid(getTotalPaidAmount(activePayment));
            summary.setRemainingAmount(getRemainingAmount(activePayment));
            summary.setIsFullyPaid(isPaymentFullyPaid(activePayment));
            summary.setActivePaymentId(activePayment.getId());

            if ("installment".equals(activePayment.getPaymentType())) {
                // Set installment plan details
                InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(activePayment.getInstallmentPlanId());
                summary.setInstallmentPlan(plan);
                summary.setCurrentInstallment(getCurrentInstallmentNumber(activePayment));
                summary.setTotalInstallments(activePayment.getTotalInstallments());

                // Calculate next installment amount
                if (!isPaymentFullyPaid(activePayment) && getCurrentInstallmentNumber(activePayment) <= activePayment.getTotalInstallments()) {
                    Double nextAmount = calculateInstallmentAmount(plan, getCurrentInstallmentNumber(activePayment), order.getTotalPrice());
                    summary.setNextInstallmentAmount(nextAmount);
                }

                summary.setPaymentStatus("partial");
            } else {
                summary.setPaymentStatus(activePayment.getStatus());
            }

            // Set deadline information
            LocalDateTime eventDateTime = parseEventDateTime(order);
            LocalDateTime deadline = eventDateTime.minusHours(PAYMENT_DEADLINE_HOURS);
            summary.setDeadlineDate(deadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            long hoursUntilDeadline = ChronoUnit.HOURS.between(LocalDateTime.now(), deadline);
            summary.setHoursUntilDeadline(Math.max(0, hoursUntilDeadline));
            summary.setIsDeadlinePassed(hoursUntilDeadline <= 0);

            // Get payment list
            List<Installment> installments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(activePayment.getId());
            List<PaymentResponse> paymentResponses = installments.stream()
                    .map(this::convertInstallmentToPaymentResponse)
                    .collect(Collectors.toList());
            summary.setPayments(paymentResponses);

        } else {
            // No active payment
            summary.setTotalPaid(0.0);
            summary.setRemainingAmount(order.getTotalPrice());
            summary.setIsFullyPaid(false);
            summary.setPaymentStatus("pending");
            summary.setPayments(new ArrayList<>());
        }

        logger.info("Payment summary generated: totalPaid={}, remaining={}, status={}",
                summary.getTotalPaid(), summary.getRemainingAmount(), summary.getPaymentStatus());

        return summary;
    }

    /**
     * Helper method to get total paid amount
     */
    private Double getTotalPaidAmount(Payment payment) {
        List<Installment> installments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(payment.getId());
        return installments.stream()
                .filter(i -> "confirmed".equals(i.getStatus()))
                .mapToDouble(Installment::getAmount)
                .sum();
    }

    /**
     * Helper method to get remaining amount
     */
    private Double getRemainingAmount(Payment payment) {
        Double totalPaid = getTotalPaidAmount(payment);
        return payment.getTotalAmount() - totalPaid;
    }

    /**
     * Helper method to check if payment is fully paid
     */
    private Boolean isPaymentFullyPaid(Payment payment) {
        return Math.abs(getRemainingAmount(payment)) < 0.01; // Account for floating point precision
    }

    /**
     * MODIFIED METHOD - Convert Installment to PaymentResponse for compatibility
     * Added better error handling and more complete data mapping
     */
    private PaymentResponse convertInstallmentToPaymentResponse(Installment installment) {
        PaymentResponse response = new PaymentResponse();

        try {
            response.setId(installment.getId()); // This is installment ID - frontend should know this
            response.setOrderId(installment.getPayment().getOrder().getId());
            response.setOrderNumber(installment.getPayment().getOrder().getOrderNumber());
            response.setAmount(installment.getAmount());
            response.setPaymentType(installment.getPayment().getPaymentType());
            response.setMethod(installment.getPaymentMethod());
            response.setSlipUrl(installment.getPaymentSlipUrl());
            response.setNotes(installment.getNotes());
            response.setStatus(installment.getStatus());
            response.setInstallmentNumber(installment.getInstallmentNumber());
            response.setIsActive(installment.getInstallmentNumber().equals(getCurrentInstallmentNumber(installment.getPayment())));

            // ENHANCED: Add customer information
            Customer customer = installment.getPayment().getOrder().getCustomer();
            if (customer != null) {
                response.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
            }

            // ENHANCED: Add event date
            EventDetails eventDetails = installment.getPayment().getOrder().getEventDetails();
            if (eventDetails != null && eventDetails.getEventDate() != null) {
                response.setEventDate(eventDetails.getEventDate());
            }

            // Format dates
            if (installment.getPaymentDate() != null) {
                response.setSubmittedDate(installment.getPaymentDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (installment.getConfirmationDate() != null) {
                response.setVerifiedDate(installment.getConfirmationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            response.setVerifiedBy(installment.getVerifiedBy());
            response.setRejectionReason(installment.getRejectionReason());

            // ENHANCED: Add remaining amount calculation
            Double totalPaid = getTotalPaidAmount(installment.getPayment());
            response.setRemainingAmount(installment.getPayment().getTotalAmount() - totalPaid);

        } catch (Exception e) {
            logger.error("Error converting installment {} to payment response: {}", installment.getId(), e.getMessage());
            // Set basic information even if there's an error
            response.setId(installment.getId());
            response.setAmount(installment.getAmount());
            response.setStatus("error");
        }

        return response;
    }

    /**
     * Create OrderResponse from Order entity
     */
    private OrderResponse createOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(String.valueOf(order.getId()));
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setTotalPrice(order.getTotalPrice());
        return response;
    }

    /**
     * Get pending payments as DTO list
     */
    public List<PaymentResponse> getPendingPaymentsAsDTO() {
        List<Installment> pendingInstallments = installmentRepository.findAll().stream()
                .filter(i -> "pending".equals(i.getStatus()) && "bank-transfer".equals(i.getPaymentMethod()))
                .collect(Collectors.toList());

        return pendingInstallments.stream()
                .map(this::convertInstallmentToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recently verified payments as DTO list
     */
    public List<PaymentResponse> getRecentlyVerifiedPaymentsAsDTO() {
        final LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        List<Installment> recentInstallments = installmentRepository.findAll().stream()
                .filter(i -> "confirmed".equals(i.getStatus()) && i.getConfirmationDate() != null)
                .filter(i -> i.getConfirmationDate().isAfter(cutoffDate))
                .collect(Collectors.toList());

        return recentInstallments.stream()
                .map(this::convertInstallmentToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * MODIFIED METHOD - Verify manual payment using installment ID directly
     * FIXED: Remove preemptive creation of next installment records
     */
    public OrderResponse verifyManualPaymentByInstallment(Long orderId, Long installmentId, boolean isApproved, String reason) {
        logger.info("Verifying manual payment by installment: orderId={}, installmentId={}, approved={}", orderId, installmentId, isApproved);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Installment not found with ID: " + installmentId));

        // Validate installment belongs to the order
        if (!installment.getPayment().getOrder().getId().equals(orderId)) {
            throw new RuntimeException("Installment does not belong to the specified order");
        }

        Payment payment = installment.getPayment();

        // Get current user for verification tracking
        String verifierName = getCurrentVerifierName();

        if (isApproved) {
            // Approve the installment
            String transactionRef = generateTransactionReference(payment, installment);
            confirmInstallmentPayment(installment, verifierName, transactionRef);

            logger.info("Approved installment {} for payment {} by {}",
                    installment.getInstallmentNumber(), payment.getId(), verifierName);

            // Check if this was the last installment
            List<Installment> allInstallments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(payment.getId());
            long confirmedCount = allInstallments.stream()
                    .filter(i -> "confirmed".equals(i.getStatus()))
                    .count();

            if (confirmedCount == payment.getTotalInstallments()) {
                // All installments completed
                payment.setStatus("completed");
                order.setPaymentStatus("completed");
                logger.info("All installments completed for payment: {}", payment.getId());
            } else {
                // FIXED: Only update current installment number, don't create next installment record
                Integer nextInstallmentNumber = installment.getInstallmentNumber() + 1;
                if (nextInstallmentNumber <= payment.getTotalInstallments()) {
                    payment.setCurrentInstallment(nextInstallmentNumber);
                    logger.info("Updated current installment to: {} (no record created yet)", nextInstallmentNumber);
                }
                payment.setStatus("partial");
                order.setPaymentStatus("partial");
            }

        } else {
            // Reject the installment
            rejectInstallmentPayment(installment, verifierName, reason);
            payment.setStatus("rejected");
            order.setPaymentStatus("rejected");

            logger.info("Rejected installment {} with reason: {} by {}",
                    installment.getInstallmentNumber(), reason, verifierName);
        }

        // Save all changes
        installmentRepository.save(installment);
        paymentRepository.save(payment);
        orderRepository.save(order);

        logger.info("Payment verification completed successfully for installment: {}", installment.getId());
        return createOrderResponse(order);
    }


    /**
     * Get payment mapper for compatibility
     */
    public PaymentMapper getPaymentMapper() {
        return new PaymentMapper();
    }

    /**
     * Get next installment info for an order
     */
    public Map<String, Object> getNextInstallmentInfo(Long orderId) {
        logger.info("Getting next installment info for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment activePayment = getActivePaymentForOrder(order);
        Map<String, Object> info = new HashMap<>();

        if (activePayment != null && "installment".equals(activePayment.getPaymentType())) {
            info.put("hasInstallmentPlan", true);
            info.put("currentInstallmentNumber", getCurrentInstallmentNumber(activePayment));
            info.put("totalInstallments", activePayment.getTotalInstallments());

            // Calculate progress information
            List<Installment> installments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(activePayment.getId());
            long confirmedCount = installments.stream()
                    .filter(i -> "confirmed".equals(i.getStatus()))
                    .count();

            info.put("completedInstallments", confirmedCount);
            info.put("remainingInstallments", activePayment.getTotalInstallments() - confirmedCount);
            info.put("progressPercentage", (confirmedCount * 100.0) / activePayment.getTotalInstallments());

            // Get totals
            info.put("totalPaid", getTotalPaidAmount(activePayment));
            info.put("remainingAmount", getRemainingAmount(activePayment));
            info.put("isFullyPaid", isPaymentFullyPaid(activePayment));

            // Get next installment details if not fully paid
            if (!isPaymentFullyPaid(activePayment)) {
                try {
                    InstallmentPlan plan = installmentPlanService.getInstallmentPlanById(activePayment.getInstallmentPlanId());
                    Integer currentInstallmentNumber = getCurrentInstallmentNumber(activePayment);

                    if (currentInstallmentNumber <= activePayment.getTotalInstallments()) {
                        Double nextAmount = calculateInstallmentAmount(plan, currentInstallmentNumber, order.getTotalPrice());
                        Double percentage = plan.getPercentages().get(currentInstallmentNumber - 1);

                        info.put("nextInstallmentAmount", nextAmount);
                        info.put("nextInstallmentPercentage", percentage);
                        info.put("nextInstallmentNumber", currentInstallmentNumber);

                        // Check if current installment has been submitted for verification
                        Installment currentInstallment = installmentRepository.findByPaymentIdAndInstallmentNumber(
                                activePayment.getId(), currentInstallmentNumber);

                        if (currentInstallment != null) {
                            info.put("currentInstallmentStatus", currentInstallment.getStatus());
                            info.put("currentInstallmentSubmitted", currentInstallment.getPaymentDate() != null);
                            info.put("currentInstallmentSlipUrl", currentInstallment.getPaymentSlipUrl());
                            info.put("currentInstallmentNotes", currentInstallment.getNotes());

                            if (currentInstallment.getPaymentDate() != null) {
                                info.put("currentInstallmentSubmittedDate",
                                        currentInstallment.getPaymentDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                            }

                            if (currentInstallment.getConfirmationDate() != null) {
                                info.put("currentInstallmentVerifiedDate",
                                        currentInstallment.getConfirmationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                            }

                            info.put("currentInstallmentVerifiedBy", currentInstallment.getVerifiedBy());
                            info.put("currentInstallmentRejectionReason", currentInstallment.getRejectionReason());
                        } else {
                            info.put("currentInstallmentStatus", "not_created");
                            info.put("currentInstallmentSubmitted", false);
                        }

                        // Add installment plan details
                        info.put("installmentPlanId", plan.getId());
                        info.put("installmentPlanName", plan.getName());
                        info.put("installmentPlanDescription", plan.getDescription());
                    } else {
                        // All installments are completed
                        info.put("nextInstallmentAmount", 0.0);
                        info.put("nextInstallmentPercentage", 0.0);
                        info.put("nextInstallmentNumber", null);
                        info.put("allInstallmentsCompleted", true);
                    }

                } catch (Exception e) {
                    logger.error("Error calculating next installment details for order {}: {}", orderId, e.getMessage());
                    info.put("error", "Could not calculate next installment details");
                }
            } else {
                // Payment is fully completed
                info.put("nextInstallmentAmount", 0.0);
                info.put("nextInstallmentPercentage", 0.0);
                info.put("nextInstallmentNumber", null);
                info.put("allInstallmentsCompleted", true);
                info.put("paymentCompleted", true);
            }

            // Add payment deadline information
            try {
                LocalDateTime eventDateTime = parseEventDateTime(order);
                LocalDateTime deadline = eventDateTime.minusHours(PAYMENT_DEADLINE_HOURS);
                info.put("paymentDeadline", deadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                long hoursUntilDeadline = ChronoUnit.HOURS.between(LocalDateTime.now(), deadline);
                info.put("hoursUntilDeadline", Math.max(0, hoursUntilDeadline));
                info.put("isDeadlinePassed", hoursUntilDeadline <= 0);
                info.put("isUrgent", hoursUntilDeadline <= 12 && hoursUntilDeadline > 0);

            } catch (Exception e) {
                logger.warn("Could not calculate payment deadline for order {}: {}", orderId, e.getMessage());
            }

            logger.info("Order {} has installment plan: {}/{} installments completed, current: {}, remaining amount: {}",
                    orderId, confirmedCount, activePayment.getTotalInstallments(),
                    getCurrentInstallmentNumber(activePayment), getRemainingAmount(activePayment));

        } else if (activePayment != null && "full".equals(activePayment.getPaymentType())) {
            // Full payment case
            info.put("hasInstallmentPlan", false);
            info.put("paymentType", "full");
            info.put("totalAmount", activePayment.getTotalAmount());
            info.put("totalPaid", getTotalPaidAmount(activePayment));
            info.put("remainingAmount", getRemainingAmount(activePayment));
            info.put("isFullyPaid", isPaymentFullyPaid(activePayment));
            info.put("paymentStatus", activePayment.getStatus());

            // Check if payment slip has been submitted
            List<Installment> installments = installmentRepository.findByPaymentIdOrderByInstallmentNumber(activePayment.getId());
            if (!installments.isEmpty()) {
                Installment singlePayment = installments.get(0);
                info.put("paymentSubmitted", singlePayment.getPaymentDate() != null);
                info.put("paymentStatus", singlePayment.getStatus());
                info.put("paymentSlipUrl", singlePayment.getPaymentSlipUrl());
                info.put("paymentNotes", singlePayment.getNotes());

                if (singlePayment.getPaymentDate() != null) {
                    info.put("paymentSubmittedDate",
                            singlePayment.getPaymentDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }

                if (singlePayment.getConfirmationDate() != null) {
                    info.put("paymentVerifiedDate",
                            singlePayment.getConfirmationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }

                info.put("paymentVerifiedBy", singlePayment.getVerifiedBy());
                info.put("paymentRejectionReason", singlePayment.getRejectionReason());
            } else {
                info.put("paymentSubmitted", false);
                info.put("paymentStatus", "not_created");
            }

            logger.info("Order {} has full payment: amount={}, paid={}, status={}",
                    orderId, activePayment.getTotalAmount(), getTotalPaidAmount(activePayment), activePayment.getStatus());

        } else {
            // No active payment
            info.put("hasInstallmentPlan", false);
            info.put("hasActivePayment", false);
            info.put("message", "No active payment found for this order");

            logger.info("Order {} has no active payment", orderId);
        }

        // Add order-level information
        info.put("orderId", orderId);
        info.put("orderNumber", order.getOrderNumber());
        info.put("orderStatus", order.getStatus());
        info.put("orderTotalPrice", order.getTotalPrice());
        info.put("orderPaymentStatus", order.getPaymentStatus());

        // Add event information if available
        if (order.getEventDetails() != null) {
            info.put("eventDate", order.getEventDetails().getEventDate());
            info.put("eventTime", order.getEventDetails().getEventTime());
            info.put("eventVenue", order.getEventDetails().getVenue());
        }

        return info;
    }
}