package com.example.sdpbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderRequest {
    private String designId;
    private String orderType;
    private String status;
    private String customerId;

    @JsonProperty("customDetails")
    private CustomDetails customDetails;

    @JsonProperty("customerInfo")
    private CustomerInfo customerInfo;

    // Getters and Setters
    public String getDesignId() {
        return designId;
    }

    public void setDesignId(String designId) {
        this.designId = designId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public CustomDetails getCustomDetails() {
        return customDetails;
    }

    public void setCustomDetails(CustomDetails customDetails) {
        this.customDetails = customDetails;
    }

    public CustomerInfo getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(CustomerInfo customerInfo) {
        this.customerInfo = customerInfo;
    }

    // Inner Classes
    public static class CustomDetails {
        private String customName;
        private String customAge;
        private String venue;
        private String eventDate;
        private String eventTime;
        private String eventCategory;

        // Getters and Setters
        public String getCustomName() {
            return customName;
        }

        public void setCustomName(String customName) {
            this.customName = customName;
        }

        public String getCustomAge() {
            return customAge;
        }

        public void setCustomAge(String customAge) {
            this.customAge = customAge;
        }

        public String getVenue() {
            return venue;
        }

        public void setVenue(String venue) {
            this.venue = venue;
        }

        public String getEventDate() {
            return eventDate;
        }

        public void setEventDate(String eventDate) {
            this.eventDate = eventDate;
        }

        public String getEventTime() {
            return eventTime;
        }

        public void setEventTime(String eventTime) {
            this.eventTime = eventTime;
        }

        public String getEventCategory() {
            return eventCategory;
        }

        public void setEventCategory(String eventCategory) {
            this.eventCategory = eventCategory;
        }
    }

    public static class CustomerInfo {
        private String firstName;
        private String lastName;
        private String email;
        private String contact;
        private String relationshipToPerson;

        // Getters and Setters
        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getContact() {
            return contact;
        }

        public void setContact(String contact) {
            this.contact = contact;
        }

        public String getRelationshipToPerson() {
            return relationshipToPerson;
        }

        public void setRelationshipToPerson(String relationshipToPerson) {
            this.relationshipToPerson = relationshipToPerson;
        }
    }
}
