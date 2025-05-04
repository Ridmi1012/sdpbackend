package com.example.sdpbackend.util;


import com.example.sdpbackend.dto.OrderRequest;
import com.example.sdpbackend.dto.OrderResponse;
import com.example.sdpbackend.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    public Order toEntity(OrderRequest request) {
        Order order = new Order();
        order.setDesignId(request.getDesignId());
        order.setOrderType(request.getOrderType());
        order.setStatus(request.getStatus());
        order.setCustomerId(request.getCustomerId());

        // Map custom details
        if (request.getCustomDetails() != null) {
            order.setCustomName(request.getCustomDetails().getCustomName());
            order.setCustomAge(request.getCustomDetails().getCustomAge());
            order.setVenue(request.getCustomDetails().getVenue());
            order.setEventDate(request.getCustomDetails().getEventDate());
            order.setEventTime(request.getCustomDetails().getEventTime());
            order.setEventCategory(request.getCustomDetails().getEventCategory());
        }

        // Map customer info
        if (request.getCustomerInfo() != null) {
            order.setFirstName(request.getCustomerInfo().getFirstName());
            order.setLastName(request.getCustomerInfo().getLastName());
            order.setEmail(request.getCustomerInfo().getEmail());
            order.setContact(request.getCustomerInfo().getContact());
            order.setRelationshipToPerson(request.getCustomerInfo().getRelationshipToPerson());
        }

        // Set default payment status
        order.setPaymentStatus("pending");

        return order;
    }

    public OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.set_id(order.getId().toString());
        response.setOrderNumber(order.getOrderNumber());
        response.setDesignId(order.getDesignId());
        response.setOrderType(order.getOrderType());
        response.setStatus(order.getStatus());
        response.setCustomerId(order.getCustomerId());

        // Map custom details
        OrderResponse.CustomDetails customDetails = new OrderResponse.CustomDetails();
        customDetails.setCustomName(order.getCustomName());
        customDetails.setCustomAge(order.getCustomAge());
        customDetails.setVenue(order.getVenue());
        customDetails.setEventDate(order.getEventDate());
        customDetails.setEventTime(order.getEventTime());
        customDetails.setEventCategory(order.getEventCategory());
        response.setCustomDetails(customDetails);

        // Map customer info
        OrderResponse.CustomerInfo customerInfo = new OrderResponse.CustomerInfo();
        customerInfo.setFirstName(order.getFirstName());
        customerInfo.setLastName(order.getLastName());
        customerInfo.setEmail(order.getEmail());
        customerInfo.setContact(order.getContact());
        customerInfo.setRelationshipToPerson(order.getRelationshipToPerson());
        response.setCustomerInfo(customerInfo);

        // Map pricing and payment info
        response.setBasePrice(order.getBasePrice());
        response.setTransportationCost(order.getTransportationCost());
        response.setAdditionalRentalCost(order.getAdditionalRentalCost());
        response.setTotalPrice(order.getTotalPrice());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCancellationReason(order.getCancellationReason());

        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        return response;
    }
}
