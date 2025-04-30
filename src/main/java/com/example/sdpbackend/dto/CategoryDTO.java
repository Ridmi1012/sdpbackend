package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class CategoryDTO {

    public static class CategoryRequest {
        private String name;
        private String description;

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
    }

    public static class CategoryResponse {
        private Integer categoryID;
        private String name;
        private String description;

        // Constructor
        public CategoryResponse(Integer categoryID, String name, String description) {
            this.categoryID = categoryID;
            this.name = name;
            this.description = description;
        }

        // Getters and setters
        public Integer getCategoryID() {
            return categoryID;
        }

        public void setCategoryID(Integer categoryID) {
            this.categoryID = categoryID;
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
    }
}