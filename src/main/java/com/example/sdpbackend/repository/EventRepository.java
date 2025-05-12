package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrderId(Long orderId);
    List<Event> findByCustomerId(String customerId);
    List<Event> findByEventDate(LocalDate date);
    long countByEventDate(LocalDate date);
    List<Event> findByEventDateBetween(LocalDate startDate, LocalDate endDate);
    List<Event> findByEventDateGreaterThanEqualOrderByEventDate(LocalDate date);
    List<Event> findByIsFullyPaid(Boolean isFullyPaid);
}
