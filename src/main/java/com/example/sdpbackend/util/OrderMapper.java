package com.example.sdpbackend.util;


import com.example.sdpbackend.dto.OrderRequest;
import com.example.sdpbackend.dto.OrderResponse;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.EventDetails;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {
    /**
     * Convert Order entity to OrderResponse DTO
     * MODIFIED - Added mapping for full-custom fields
     */
    public OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();

        // Map basic order fields
        response.set_id(order.getId().toString());
        response.setOrderNumber(order.getOrderNumber());
        response.setDesignId(String.valueOf(order.getDesignId()));
        response.setOrderType(order.getOrderType());
        response.setStatus(order.getStatus());
        response.setCustomerId(order.getCustomer().getcustomerId().toString());
        response.setBasePrice(order.getBasePrice());
        response.setTransportationCost(order.getTransportationCost());
        response.setAdditionalRentalCost(order.getAdditionalRentalCost());
        response.setTotalPrice(order.getTotalPrice());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCancellationReason(order.getCancellationReason());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // Map request-similar and full-custom specific fields
        response.setThemeColor(order.getThemeColor());
        response.setConceptCustomization(order.getConceptCustomization());

        // NEW - Map full-custom specific fields
        if ("full-custom".equals(order.getOrderType())) {
            response.setInspirationPhotos(order.getInspirationPhotos());
            response.setSpecialNote(order.getSpecialNote());
        }

        // Map order items for request-similar and full-custom orders
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<OrderResponse.OrderItemResponse> itemResponses = order.getOrderItems().stream()
                    .map(this::toOrderItemResponse)
                    .collect(Collectors.toList());
            response.setOrderItems(itemResponses);
        }

        // Map event details if available
        if (order.getEventDetails() != null) {
            EventDetails eventDetails = order.getEventDetails();
            OrderResponse.CustomDetails customDetails = new OrderResponse.CustomDetails();

            customDetails.setCustomName(eventDetails.getCustomName());
            customDetails.setCustomAge(eventDetails.getCustomAge());
            customDetails.setVenue(eventDetails.getVenue());
            customDetails.setEventDate(eventDetails.getEventDate());
            customDetails.setEventTime(eventDetails.getEventTime());
            customDetails.setEventCategory(eventDetails.getEventCategory());

            response.setCustomDetails(customDetails);
        }

        // Map customer info
        if (order.getCustomer() != null) {
            OrderResponse.CustomerInfo customerInfo = new OrderResponse.CustomerInfo();

            customerInfo.setFirstName(order.getCustomer().getFirstName());
            customerInfo.setLastName(order.getCustomer().getLastName());
            customerInfo.setEmail(order.getCustomer().getEmail());
            customerInfo.setContact(order.getCustomer().getContact());

            // Relationship info might be in event details
            if (order.getEventDetails() != null && order.getEventDetails().getRelationshipToPerson() != null) {
                customerInfo.setRelationshipToPerson(order.getEventDetails().getRelationshipToPerson());
            }

            response.setCustomerInfo(customerInfo);
        }

        return response;
    }

    private OrderResponse.OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        OrderResponse.OrderItemResponse response = new OrderResponse.OrderItemResponse();
        response.setId(orderItem.getId());
        response.setItemId(orderItem.getItemId());
        response.setItemName(orderItem.getItemName());
        response.setItemCategory(orderItem.getItemCategory());
        response.setQuantity(orderItem.getQuantity());
        response.setPricePerUnit(orderItem.getPricePerUnit());
        response.setTotalPrice(orderItem.getTotalPrice());
        response.setStatus(orderItem.getStatus());
        return response;
    }

    /**
     * Convert OrderRequest DTO to Order entity (for creation)
     * MODIFIED - Added handling for full-custom fields
     */
    public Order toEntity(OrderRequest request, Customer customer) {
        Order order = new Order();

        // Set basic order properties
        order.setDesignId(Long.valueOf(request.getDesignId()));
        order.setOrderType(request.getOrderType());
        order.setCustomer(customer);
        order.setStatus(request.getStatus() != null ? request.getStatus() : "pending");

        // Set customization fields for both request-similar and full-custom
        if ("request-similar".equals(request.getOrderType()) || "full-custom".equals(request.getOrderType())) {
            order.setThemeColor(request.getThemeColor());
            order.setConceptCustomization(request.getConceptCustomization());
        }

        // NEW - Set full-custom specific fields
        if ("full-custom".equals(request.getOrderType())) {
            order.setSpecialNote(request.getSpecialNote());

            // Handle inspiration photos (max 3)
            if (request.getInspirationPhotos() != null) {
                List<String> photos = request.getInspirationPhotos().stream()
                        .limit(3)
                        .collect(Collectors.toList());
                order.setInspirationPhotos(photos);
            }
        }

        // Create and set event details
        if (request.getCustomDetails() != null) {
            EventDetails eventDetails = new EventDetails();
            eventDetails.setOrder(order);

            eventDetails.setCustomName(request.getCustomDetails().getCustomName());
            eventDetails.setCustomAge(request.getCustomDetails().getCustomAge());
            eventDetails.setVenue(request.getCustomDetails().getVenue());
            eventDetails.setEventDate(request.getCustomDetails().getEventDate());
            eventDetails.setEventTime(request.getCustomDetails().getEventTime());
            eventDetails.setEventCategory(request.getCustomDetails().getEventCategory());

            // Get relationship info from the appropriate place
            if (request.getCustomDetails().getRelationshipToPerson() != null) {
                eventDetails.setRelationshipToPerson(request.getCustomDetails().getRelationshipToPerson());
            } else if (request.getCustomerInfo() != null && request.getCustomerInfo().getRelationshipToPerson() != null) {
                eventDetails.setRelationshipToPerson(request.getCustomerInfo().getRelationshipToPerson());
            }

            order.setEventDetails(eventDetails);
        }

        return order;
    }

    /**
     * Update an existing Order entity with data from an OrderRequest
     * MODIFIED - Added handling for full-custom fields
     */
    public Order updateEntityFromRequest(Order order, OrderRequest request) {
        // Only update fields that are present in the request
        if (request.getOrderType() != null) {
            order.setOrderType(request.getOrderType());
        }

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        // Update customization fields for request-similar and full-custom
        if ("request-similar".equals(order.getOrderType()) || "full-custom".equals(order.getOrderType())) {
            if (request.getThemeColor() != null) {
                order.setThemeColor(request.getThemeColor());
            }
            if (request.getConceptCustomization() != null) {
                order.setConceptCustomization(request.getConceptCustomization());
            }
        }

        // NEW - Update full-custom specific fields
        if ("full-custom".equals(order.getOrderType())) {
            if (request.getSpecialNote() != null) {
                order.setSpecialNote(request.getSpecialNote());
            }
            if (request.getInspirationPhotos() != null) {
                List<String> photos = request.getInspirationPhotos().stream()
                        .limit(3)
                        .collect(Collectors.toList());
                order.setInspirationPhotos(photos);
            }
        }

        // Update event details if present
        if (request.getCustomDetails() != null) {
            EventDetails eventDetails = order.getEventDetails();
            if (eventDetails == null) {
                eventDetails = new EventDetails();
                eventDetails.setOrder(order);
                order.setEventDetails(eventDetails);
            }

            OrderRequest.CustomDetails requestDetails = request.getCustomDetails();

            if (requestDetails.getCustomName() != null) {
                eventDetails.setCustomName(requestDetails.getCustomName());
            }

            if (requestDetails.getCustomAge() != null) {
                eventDetails.setCustomAge(requestDetails.getCustomAge());
            }

            if (requestDetails.getVenue() != null) {
                eventDetails.setVenue(requestDetails.getVenue());
            }

            if (requestDetails.getEventDate() != null) {
                eventDetails.setEventDate(requestDetails.getEventDate());
            }

            if (requestDetails.getEventTime() != null) {
                eventDetails.setEventTime(requestDetails.getEventTime());
            }

            if (requestDetails.getEventCategory() != null) {
                eventDetails.setEventCategory(requestDetails.getEventCategory());
            }

            if (requestDetails.getRelationshipToPerson() != null) {
                eventDetails.setRelationshipToPerson(requestDetails.getRelationshipToPerson());
            }
        }

        return order;
    }
}
