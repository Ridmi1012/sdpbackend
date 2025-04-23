package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


public class DesignDTO {
    // Request DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesignRequest {
        private String name;
        private Integer categoryId;
        private BigDecimal basePrice;
        private String description;
        private String imageUrl;
        private Integer createdBy;
        private List<DesignItemDTO.DesignItemRequest> items;
    }

    // Response DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesignResponse {
        private Integer designID;
        private String name;
        private CategoryDTO.CategoryResponse category;
        private BigDecimal basePrice;
        private String description;
        private String imageUrl;
        private List<DesignItemDTO.DesignItemResponse> items;
    }
}
