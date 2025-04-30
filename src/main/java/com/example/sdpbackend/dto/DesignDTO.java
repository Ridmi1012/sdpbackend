package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


public class DesignDTO {

    public static class DesignRequest {
        private String name;
        private Integer categoryId;
        private BigDecimal basePrice;
        private String description;
        private String imageUrl;
        private Integer createdBy;
        private List<DesignItemDTO.DesignItemRequest> items;
        private List<String> additionalImages;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Integer categoryId) {
            this.categoryId = categoryId;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public Integer getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(Integer createdBy) {
            this.createdBy = createdBy;
        }

        public List<DesignItemDTO.DesignItemRequest> getItems() {
            return items;
        }

        public void setItems(List<DesignItemDTO.DesignItemRequest> items) {
            this.items = items;
        }

        public List<String> getAdditionalImages() {
            return additionalImages;
        }

        public void setAdditionalImages(List<String> additionalImages) {
            this.additionalImages = additionalImages;
        }
    }

    public static class DesignResponse {
        private Integer designID;
        private String name;
        private CategoryDTO.CategoryResponse category;
        private BigDecimal basePrice;
        private String description;
        private String imageUrl;
        private List<DesignItemDTO.DesignItemResponse> items;
        private List<String> additionalImages;

        // Constructor without additionalImages
        public DesignResponse(Integer designID, String name, CategoryDTO.CategoryResponse category,
                              BigDecimal basePrice, String description, String imageUrl,
                              List<DesignItemDTO.DesignItemResponse> items) {
            this.designID = designID;
            this.name = name;
            this.category = category;
            this.basePrice = basePrice;
            this.description = description;
            this.imageUrl = imageUrl;
            this.items = items;
        }

        // Constructor with additionalImages
        public DesignResponse(Integer designID, String name, CategoryDTO.CategoryResponse category,
                              BigDecimal basePrice, String description, String imageUrl,
                              List<DesignItemDTO.DesignItemResponse> items, List<String> additionalImages) {
            this.designID = designID;
            this.name = name;
            this.category = category;
            this.basePrice = basePrice;
            this.description = description;
            this.imageUrl = imageUrl;
            this.items = items;
            this.additionalImages = additionalImages;
        }

        // Getters and setters
        public Integer getDesignID() {
            return designID;
        }

        public void setDesignID(Integer designID) {
            this.designID = designID;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public CategoryDTO.CategoryResponse getCategory() {
            return category;
        }

        public void setCategory(CategoryDTO.CategoryResponse category) {
            this.category = category;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public List<DesignItemDTO.DesignItemResponse> getItems() {
            return items;
        }

        public void setItems(List<DesignItemDTO.DesignItemResponse> items) {
            this.items = items;
        }

        public List<String> getAdditionalImages() {
            return additionalImages;
        }

        public void setAdditionalImages(List<String> additionalImages) {
            this.additionalImages = additionalImages;
        }
    }
}