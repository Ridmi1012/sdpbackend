package com.example.sdpbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inspiration_images")
public class InspirationImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private String imageUrl;

    private String description;

    // Getters and setters
    // ...
}
