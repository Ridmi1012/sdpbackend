package com.example.sdpbackend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    private String customerId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate eventDate;

    private LocalTime eventTime;

    private String location;

    @Column(nullable = false)
    private String status;  // 'scheduled', 'in-progress', 'completed', 'cancelled'


    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private Boolean isFullyPaid = false;
}
