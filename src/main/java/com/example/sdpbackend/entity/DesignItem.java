package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "design_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer designItemID;

    @ManyToOne
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    private int defaultQuantity;

    private boolean isOptional;
}
