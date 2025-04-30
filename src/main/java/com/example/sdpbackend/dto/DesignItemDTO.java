package com.example.sdpbackend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class DesignItemDTO {

    public static class DesignItemRequest {
        private Integer itemId;
        private int defaultQuantity;
        private boolean isOptional;

        // Getters and setters
        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public int getDefaultQuantity() {
            return defaultQuantity;
        }

        public void setDefaultQuantity(int defaultQuantity) {
            this.defaultQuantity = defaultQuantity;
        }

        public boolean isOptional() {
            return isOptional;
        }

        public void setOptional(boolean optional) {
            isOptional = optional;
        }
    }

    public static class DesignItemResponse {
        private Integer designItemID;
        private ItemDTO.ItemResponse item;
        private int defaultQuantity;
        private boolean isOptional;

        // Constructor
        public DesignItemResponse(Integer designItemID, ItemDTO.ItemResponse item, int defaultQuantity, boolean isOptional) {
            this.designItemID = designItemID;
            this.item = item;
            this.defaultQuantity = defaultQuantity;
            this.isOptional = isOptional;
        }

        // Getters and setters
        public Integer getDesignItemID() {
            return designItemID;
        }

        public void setDesignItemID(Integer designItemID) {
            this.designItemID = designItemID;
        }

        public ItemDTO.ItemResponse getItem() {
            return item;
        }

        public void setItem(ItemDTO.ItemResponse item) {
            this.item = item;
        }

        public int getDefaultQuantity() {
            return defaultQuantity;
        }

        public void setDefaultQuantity(int defaultQuantity) {
            this.defaultQuantity = defaultQuantity;
        }

        public boolean isOptional() {
            return isOptional;
        }

        public void setOptional(boolean optional) {
            isOptional = optional;
        }
    }
}