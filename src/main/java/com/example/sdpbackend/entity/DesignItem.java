package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Design_Items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer designItemID;

    @ManyToOne
    @JoinColumn(name = "designID", nullable = false)
    private Design design;

    @ManyToOne
    @JoinColumn(name = "itemID", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer defaultQuantity = 1;
}
