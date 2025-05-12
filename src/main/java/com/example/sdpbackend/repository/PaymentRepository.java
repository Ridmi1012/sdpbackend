package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByStatus(String status);
    long countByStatus(String status);
    List<Payment> findByStatusAndMethodNot(String status, String excludeMethod);
    long countByStatusAndMethodNot(String status, String excludeMethod);
    List<Payment> findByStatusAndConfirmationDateTimeAfter(String status, LocalDateTime dateTime);

    // Find payments by installment plan ID
    List<Payment> findByInstallmentPlanId(Integer installmentPlanId);

    // Find payments by order ID and installment number
    List<Payment> findByOrderIdAndInstallmentNumber(Long orderId, Integer installmentNumber);

    // Count number of completed payments for an order
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.order.id = :orderId AND p.status = 'completed'")
    long countCompletedPaymentsByOrderId(@Param("orderId") Long orderId);

    // Calculate total amount paid for an order
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.order.id = :orderId AND p.status = 'completed'")
    Double sumCompletedPaymentAmountsByOrderId(@Param("orderId") Long orderId);

    List<Payment> findByMethod(String method);

    // Find latest payment for an order
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status = 'completed' ORDER BY p.confirmationDateTime DESC")
    List<Payment> findLatestCompletedPaymentByOrderId(@Param("orderId") Long orderId);

    // Find payments by order and date range
    List<Payment> findByOrderIdAndConfirmationDateTimeBetween(Long orderId, LocalDateTime start, LocalDateTime end);
}
