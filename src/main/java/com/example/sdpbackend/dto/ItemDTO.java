package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class ItemDTO {

    public static class ItemRequest {
        private String name;
        private String description;
        private BigDecimal unitPrice;
        private String imageUrl;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

    }

    public static class ItemResponse {
        private Integer itemID;
        private String name;
        private String description;
        private BigDecimal unitPrice;


        // Constructor without imageUrl (for backward compatibility)
        public ItemResponse(Integer itemID, String name, String description, BigDecimal unitPrice) {
            this.itemID = itemID;
            this.name = name;
            this.description = description;
            this.unitPrice = unitPrice;
        }

        // Constructor with imageUrl
        public ItemResponse(Integer itemID, String name, String description, BigDecimal unitPrice, String imageUrl) {
            this.itemID = itemID;
            this.name = name;
            this.description = description;
            this.unitPrice = unitPrice;

        }

        // Getters and setters
        public Integer getItemID() {
            return itemID;
        }

        public void setItemID(Integer itemID) {
            this.itemID = itemID;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

    }
}
