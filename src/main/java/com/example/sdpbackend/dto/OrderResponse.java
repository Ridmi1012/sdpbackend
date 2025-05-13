package com.example.sdpbackend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
        }

    public Object getPayHereParams() {
        return payHereParams;
    }

    // This method is for compatibility if needed
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
}