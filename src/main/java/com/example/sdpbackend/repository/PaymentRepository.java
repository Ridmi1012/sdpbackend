package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByStatus(String status);
    long countByStatus(String status);
    List<Payment> findByStatusAndConfirmationDateTimeAfter(String status, LocalDateTime dateTime);
}
