package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class CategoryDTO {
    // Request DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRequest {
        private String name;
        private String description;
    }

    // Response DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Integer categoryID;
        private String name;
        private String description;
    }
}
