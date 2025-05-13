package com.example.sdpbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String designId;
    private String orderType;
    private String status;

    @JsonProperty("customDetails")
    private CustomDetails customDetails;

    @JsonProperty("customerInfo")
    private CustomerInfo customerInfo;

    // NEW FIELDS for request-similar
    private String themeColor;
    private String conceptCustomization;

    // NEW - List of items for request-similar orders
    @JsonProperty("orderItems")
    private List<OrderItemRequest> orderItems;

    // Inner Classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomDetails {
        private String customName;
        private String customAge;
        private String venue;
        private String eventDate;
        private String eventTime;
        private String eventCategory;
        private String relationshipToPerson;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String firstName;
        private String lastName;
        private String email;
        private String contact;
        private String relationshipToPerson;
    }

    // NEW INNER CLASS for order items
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Long itemId;
        private String itemName;
        private String itemCategory;
        private Integer quantity;
        private Double pricePerUnit;
        private String status; // 'active' or 'dropped'
    }
}
