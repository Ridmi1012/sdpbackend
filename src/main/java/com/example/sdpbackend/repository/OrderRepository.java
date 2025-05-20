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

    List<Order> findByStatus(String status);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND EXISTS (SELECT e FROM Event e WHERE e.orderId = o.id)")
    List<Order> findByStatusAndHasEvents(@Param("status") String status);

    List<Order> findByCustomerCustomerIdAndStatusIn(Integer customerId, List<String> statuses);

    List<Order> findByStatusIn(List<String> statuses);

    List<Order> findByCustomerCustomerIdAndDesignIdAndStatusIn(Integer customerId, Long designId, List<String> statuses);
}
