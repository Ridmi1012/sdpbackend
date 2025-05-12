package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerCustomerId(Integer customerId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(String status);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND EXISTS (SELECT e FROM Event e WHERE e.orderId = o.id)")
    List<Order> findByStatusAndHasEvents(@Param("status") String status);

    List<Order> findByCustomerCustomerIdAndStatus(Integer customerId, String status);

    List<Order> findByCustomerCustomerIdAndStatusIn(Integer customerId, List<String> statuses);

    List<Order> findByStatusIn(List<String> statuses);

    Long countByStatus(String status);

    // Find orders by payment status
    List<Order> findByPaymentStatus(String paymentStatus);

    // Find orders with installment plans
    @Query("SELECT o FROM Order o WHERE o.installmentPlanId IS NOT NULL")
    List<Order> findOrdersWithInstallmentPlans();

    // Find orders with upcoming installment due dates
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = 'partial' AND o.nextInstallmentDueDate < :deadline")
    List<Order> findOrdersWithUpcomingInstallments(@Param("deadline") LocalDateTime deadline);

    // Find orders by installment plan ID
    List<Order> findByInstallmentPlanId(Long installmentPlanId);

    // Find orders with payment due soon (24 hours before event)
    @Query("SELECT o FROM Order o JOIN o.eventDetails e WHERE o.paymentStatus != 'completed' " +
            "AND function('date_add', function('to_date', e.eventDate, 'YYYY-MM-DD'), -1) <= CURRENT_DATE")
    List<Order> findOrdersWithPaymentDueSoon();
}
