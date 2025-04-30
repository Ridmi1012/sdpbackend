package com.example.sdpbackend.dto;

import java.time.LocalDate;
import java.util.List;

public class FullyCustomOrderDTO {
    private Integer customerId;
    private String deliveryAddress;
    private LocalDate eventDate;
    private String customName;
    private Integer customAge;
    private String themeName;          // Theme name for the custom design
    private String themeColor;         // Main color scheme
    private String conceptDescription; // Detailed concept description
    private List<String> inspirationImageUrls; // URLs of inspiration images
    private List<CustomItemDTO> items; // Items to include in the design

    // DTO for custom items
    public static class CustomItemDTO {
        private Integer itemId;        // Existing item ID if using catalog item
        private String customItemName; // Name if it's a new custom item
        private String description;    // Description of the item
        private Integer quantity;      // Quantity of the item

        // Getters and Setters
        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public String getCustomItemName() {
            return customItemName;
        }

        public void setCustomItemName(String customItemName) {
            this.customItemName = customItemName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    // Getters and Setters
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

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
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

    public List<String> getInspirationImageUrls() {
        return inspirationImageUrls;
    }

    public void setInspirationImageUrls(List<String> inspirationImageUrls) {
        this.inspirationImageUrls = inspirationImageUrls;
    }

    public List<CustomItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CustomItemDTO> items) {
        this.items = items;
    }
}
