package com.example.sdpbackend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String orderNumber;
    private String designId;
    private String orderType;
    private CustomDetails customDetails;
    private CustomerInfo customerInfo;
    private String status;
    private String customerId;

    // Fields for request-similar and full-custom
    private String themeColor;
    private String conceptCustomization;

    // NEW FIELDS for full-custom
    private List<String> inspirationPhotos;
    private String specialNote;

    // List of order items
    private List<OrderItemResponse> orderItems;

    private Double basePrice;
    private Double transportationCost;
    private Double additionalRentalCost;
    private Double totalPrice;
    private String paymentStatus;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, String> payHereParams;

    public void setPayHereParams(Map<String, String> params) {
        this.payHereParams = params;
    }

    public Object getPayHereParams() {
        return payHereParams;
    }

    public void set_id(String id) {
        this.id = id;
    }

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long itemId;
        private String itemName;
        private String itemCategory;
        private Integer quantity;
        private Double pricePerUnit;
        private Double totalPrice;
        private String status;
    }
}