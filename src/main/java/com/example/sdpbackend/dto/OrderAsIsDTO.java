package com.example.sdpbackend.dto;

import java.time.LocalDate;

public class OrderAsIsDTO {
    private Integer designId;
    private Integer customerId;  // Changed from Long to Integer
    private String deliveryAddress;
    private LocalDate eventDate;
    private String customName;
    private Integer customAge;

    // Getters and setters
    public Integer getDesignId() {
        return designId;
    }

    public void setDesignId(Integer designId) {
        this.designId = designId;
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
}