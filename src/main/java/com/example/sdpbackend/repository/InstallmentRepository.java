package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long>{

    @Query("SELECT i FROM Installment i WHERE i.payment.order.id = :orderId")
    List<Installment> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(i) FROM Installment i WHERE i.status = 'pending' AND i.paymentMethod = 'bank-transfer'")
    long countPendingBankTransfers();
}
