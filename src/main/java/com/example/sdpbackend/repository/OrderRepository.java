package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.entity.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Use JPQL query to specify the exact path to the customerId
    @Query("SELECT o FROM Order o WHERE o.customer.customerId = :customerId")
    List<Order> findByCustomerId(@Param("customerId") Integer customerId);
    List<Order> findByOrderType(OrderType orderType);
    List<Order> findByOrderStatus(String status);
}
