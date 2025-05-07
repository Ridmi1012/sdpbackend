package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Custom event details
    @Column(nullable = false)
    private String customName;

    private String customAge;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String eventDate;

    private String eventTime;

    @Column(nullable = false)
    private String eventCategory;

    private String relationshipToPerson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
