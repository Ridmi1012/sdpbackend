package com.example.sdpbackend.dto;

import java.time.LocalDate;
import java.util.List;

public class CustomizedOrderDTO {
    private Integer baseDesignId;      // Original design to customize
    private Integer customerId;
    private String deliveryAddress;
    private LocalDate eventDate;
    private String customName;
    private Integer customAge;
    private String themeColor;         // Optional theme color change
    private String conceptDescription; // Description of concept changes
    private List<OrderItemDTO> addItems;     // Items to add
    private List<OrderItemDTO> removeItems;  // Item IDs to remove
    private List<OrderItemDTO> modifyItems;  // Items to modify quantity

    // DTO for items in the order
    public static class OrderItemDTO {
        private Integer itemId;
        private Integer quantity;

        // Getters and Setters
        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    // Getters and Setters
    public Integer getBaseDesignId() {
        return baseDesignId;
    }

    public void setBaseDesignId(Integer baseDesignId) {
        this.baseDesignId = baseDesignId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public Integer getCustomAge() {
        return customAge;
    }

    public void setCustomAge(Integer customAge) {
        this.customAge = customAge;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public String getConceptDescription() {
        return conceptDescription;
    }

    public void setConceptDescription(String conceptDescription) {
        this.conceptDescription = conceptDescription;
    }

    public List<OrderItemDTO> getAddItems() {
        return addItems;
    }

    public void setAddItems(List<OrderItemDTO> addItems) {
        this.addItems = addItems;
    }

    public List<OrderItemDTO> getRemoveItems() {
        return removeItems;
    }

    public void setRemoveItems(List<OrderItemDTO> removeItems) {
        this.removeItems = removeItems;
    }

    public List<OrderItemDTO> getModifyItems() {
        return modifyItems;
    }

    public void setModifyItems(List<OrderItemDTO> modifyItems) {
        this.modifyItems = modifyItems;
    }
}
