package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


public class ItemDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        private String name;
        private String description;
        private BigDecimal unitPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private Integer itemID;
        private String name;
        private String description;
        private BigDecimal unitPrice;
    }
}
