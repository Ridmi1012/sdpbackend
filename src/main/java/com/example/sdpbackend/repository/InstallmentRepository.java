package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long>{
        /**
         * Count pending bank transfer installments
         */
        @Query("SELECT COUNT(i) FROM Installment i WHERE i.status = 'pending' AND i.paymentMethod = 'bank-transfer'")
        long countPendingBankTransfers();

        /**
         * Find current installment for a payment
         */
        @Query("SELECT i FROM Installment i WHERE i.payment.id = :paymentId AND i.installmentNumber = :installmentNumber")
        Installment findByPaymentIdAndInstallmentNumber(@Param("paymentId") Long paymentId, @Param("installmentNumber") Integer installmentNumber);

        /**
         * Find installments by payment ID ordered by installment number - FIXED
         */
        @Query("SELECT i FROM Installment i WHERE i.payment.id = :paymentId ORDER BY i.installmentNumber")
        List<Installment> findByPaymentIdOrderByInstallmentNumber(@Param("paymentId") Long paymentId);

        /**
         * Get total amount of confirmed installments for a payment
         */
        @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Installment i WHERE i.payment.id = :paymentId AND i.status = 'confirmed'")
        Double getTotalConfirmedAmountByPaymentId(@Param("paymentId") Long paymentId);


        /**
         * Find installments by order ID - FIXED: Added missing implementation
         */
        @Query("SELECT i FROM Installment i WHERE i.payment.order.id = :orderId ORDER BY i.installmentNumber")
        List<Installment> findByOrderId(@Param("orderId") Long orderId);
}
