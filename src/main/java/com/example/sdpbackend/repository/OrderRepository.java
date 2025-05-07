package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerCustomerId(Integer customerId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(String status);

    List<Order> findByCustomerCustomerIdAndStatus(Integer customerId, String status);

    List<Order> findByCustomerCustomerIdAndStatusIn(Integer customerId, List<String> statuses);

    List<Order> findByStatusIn(List<String> statuses);

    Long countByStatus(String status);
}
