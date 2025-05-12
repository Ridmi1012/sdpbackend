package com.example.sdpbackend.util;


import com.example.sdpbackend.dto.OrderRequest;
import com.example.sdpbackend.dto.OrderResponse;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.EventDetails;
import com.example.sdpbackend.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    /**
     * Convert Order entity to OrderResponse DTO
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

    /**
     * Convert OrderRequest DTO to Order entity (for creation)
     */
    public Order toEntity(OrderRequest request, Customer customer) {
        Order order = new Order();

        // Set basic order properties
        order.setDesignId(Long.valueOf(request.getDesignId()));
        order.setOrderType(request.getOrderType());
        order.setCustomer(customer);
        order.setStatus(request.getStatus() != null ? request.getStatus() : "pending");

        // Create and set event details
        if (request.getCustomDetails() != null) {
            EventDetails eventDetails = new EventDetails();
            eventDetails.setOrder(order); // Establish bidirectional relationship

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
     */
    public Order updateEntityFromRequest(Order order, OrderRequest request) {
        // Only update fields that are present in the request
        if (request.getOrderType() != null) {
            order.setOrderType(request.getOrderType());
        }

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
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
