package com.example.sdpbackend.exception;

public class OrderNotFoundException extends OrderException {

    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}
