package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;  // Can be null for custom items

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "is_add_on")
    private boolean isAddOn;  // Indicates if this item was added as an extra

    // Fields for custom items (Scenario 3)
    @Column(name = "custom_item_name")
    private String customItemName;

    @Column(name = "custom_item_description", columnDefinition = "TEXT")
    private String customItemDescription;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAddOn() {
        return isAddOn;
    }

    public void setIsAddOn(boolean isAddOn) {
        this.isAddOn = isAddOn;
    }

    public String getCustomItemName() {
        return customItemName;
    }

    public void setCustomItemName(String customItemName) {
        this.customItemName = customItemName;
    }

    public String getCustomItemDescription() {
        return customItemDescription;
    }

    public void setCustomItemDescription(String customItemDescription) {
        this.customItemDescription = customItemDescription;
    }
}