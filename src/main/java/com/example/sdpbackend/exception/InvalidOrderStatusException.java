package com.example.sdpbackend.exception;

public class InvalidOrderStatusException extends OrderException {
    public InvalidOrderStatusException(String message) {
        super(message);
    }
}
