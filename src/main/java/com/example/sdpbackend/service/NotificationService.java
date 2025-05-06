package com.example.sdpbackend.service;

import com.example.sdpbackend.entity.Notification;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Store subscription information for a user
    public void saveSubscription(String username, Map<String, Object> subscriptionData) {
        try {
            // Convert subscription data to JSON string
            String subscriptionJson = objectMapper.writeValueAsString(subscriptionData.get("subscription"));

            // Find existing notification preferences for user or create new
            Notification notification = notificationRepository.findByUsername(username)
                    .orElse(new Notification());

            notification.setUsername(username);
            notification.setSubscription(subscriptionJson);
            notification.setUpdatedAt(LocalDateTime.now());

            notificationRepository.save(notification);
        } catch (IOException e) {
            throw new RuntimeException("Error processing subscription data", e);
        }
    }

    // Send a test notification to verify subscription works
    @Async
    public void sendTestNotification(String username) {
        Notification notification = notificationRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("No subscription found for user: " + username));

        // In a real implementation, this would send a push notification
        // For now, we'll just log it
        System.out.println("Sending test notification to user: " + username);
    }

    // Create a notification for a new order
    @Async
    public void createOrderNotification(Order order) {
        // Create notification for all admin users
        List<Notification> adminNotifications = notificationRepository.findByUserType("ADMIN");

        for (Notification adminNotification : adminNotifications) {
            // Create a new notification record
            Notification.NotificationRecord record = new Notification.NotificationRecord();
            record.setType("new-order");
            record.setTitle("New Order Request");
            record.setBody("Order #" + order.getOrderNumber() + " received from " +
                    order.getFirstName() + " " + order.getLastName());
            record.setOrderId(order.getId().toString());
            record.setRead(false);
            record.setCreatedAt(LocalDateTime.now());

            // Add to notification records
            adminNotification.getNotificationRecords().add(record);
            adminNotification.setUpdatedAt(LocalDateTime.now());

            // Save notification
            notificationRepository.save(adminNotification);

            // In a real implementation, this would also send a push notification
            // to the admin's browser using the subscription
            System.out.println("New order notification created for admin: " + adminNotification.getUsername());
        }
    }

    // Get count of unread notifications
    public int getUnreadNotificationsCount(String username) {
        return notificationRepository.findByUsername(username)
                .map(notification -> (int) notification.getNotificationRecords().stream()
                        .filter(record -> !record.isRead())
                        .count())
                .orElse(0);
    }

    // Mark a specific notification as read
    public void markNotificationAsRead(String username, String orderId) {
        notificationRepository.findByUsername(username).ifPresent(notification -> {
            boolean updated = false;

            for (Notification.NotificationRecord record : notification.getNotificationRecords()) {
                if (record.getOrderId().equals(orderId) && !record.isRead()) {
                    record.setRead(true);
                    record.setReadAt(LocalDateTime.now());
                    updated = true;
                }
            }

            if (updated) {
                notification.setUpdatedAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        });
    }

    // Mark all notifications as read
    public void markAllNotificationsAsRead(String username) {
        notificationRepository.findByUsername(username).ifPresent(notification -> {
            boolean updated = false;

            for (Notification.NotificationRecord record : notification.getNotificationRecords()) {
                if (!record.isRead()) {
                    record.setRead(true);
                    record.setReadAt(LocalDateTime.now());
                    updated = true;
                }
            }

            if (updated) {
                notification.setUpdatedAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        });
    }
}
