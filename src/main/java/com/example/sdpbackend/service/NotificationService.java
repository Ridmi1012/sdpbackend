package com.example.sdpbackend.service;

import com.example.sdpbackend.entity.Event;
import com.example.sdpbackend.entity.Notification;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.entity.Payment;
import com.example.sdpbackend.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    /**
     * Create notification when a new order is placed
     */
    public void createOrderNotification(Order order) {
        // Implementation for creating order notification
        // This would typically send an in-app notification, email, or SMS
        System.out.println("New order notification created for order: " + order.getOrderNumber());
    }

    /**
     * Create notification when a payment is made
     */
    public void createPaymentNotification(Order order, Payment payment) {
        // Implementation for creating payment notification
        System.out.println("Payment notification created for order: " + order.getOrderNumber() +
                ", amount: " + payment.getAmount());
    }

    /**
     * Create notification when a payment slip is uploaded
     */
    public void createPaymentSlipNotification(Order order, Payment payment) {
        // Implementation for creating payment slip notification
        System.out.println("Payment slip notification created for order: " + order.getOrderNumber() +
                ", amount: " + payment.getAmount());
    }

    /**
     * Create notification when a payment is verified
     */
    public void createPaymentVerificationNotification(Order order, Payment payment, boolean isApproved) {
        // Implementation for creating payment verification notification
        String status = isApproved ? "approved" : "rejected";
        System.out.println("Payment verification notification created for order: " + order.getOrderNumber() +
                ", amount: " + payment.getAmount() + ", status: " + status);
    }

    /**
     * Create notification when an event is created from an order
     */
    public void createEventNotification(Order order, Event event) {
        // Implementation for creating event notification
        System.out.println("Event notification created for order: " + order.getOrderNumber() +
                ", event date: " + event.getEventDate());
    }
}




