package com.example.sdpbackend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class DesignItemDTO {
    // Request DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesignItemRequest {
        private Integer itemId;
        private Integer defaultQuantity;
    }

    // Response DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesignItemResponse {
        private Integer designItemID;
        private ItemDTO.ItemResponse item;
        private Integer defaultQuantity;
    }
}
