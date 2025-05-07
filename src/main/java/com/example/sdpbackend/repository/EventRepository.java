package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByEventDate(LocalDate date);

    List<Event> findByEventDateBetween(LocalDate startDate, LocalDate endDate);

    List<Event> findByCustomerId(String customerId);

    List<Event> findByOrderId(Long orderId);

    List<Event> findByStatus(String status);

    // Find today's events - useful for dashboard
    List<Event> findByEventDateAndStatus(LocalDate date, String status);

    // Count today's events - useful for notifications
    long countByEventDate(LocalDate date);

    // Find upcoming events
    List<Event> findByEventDateGreaterThanEqualOrderByEventDate(LocalDate date);

    // Find past events
    List<Event> findByEventDateLessThanOrderByEventDateDesc(LocalDate date);
}
