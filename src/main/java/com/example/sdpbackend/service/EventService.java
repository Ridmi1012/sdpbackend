package com.example.sdpbackend.service;

import com.example.sdpbackend.entity.Event;
import com.example.sdpbackend.entity.EventDetails;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.repository.EventRepository;
import com.example.sdpbackend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationService notificationService;

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<Event> getEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        return eventRepository.findByEventDateBetween(startDate, endDate);
    }

    public List<Event> getTodayEvents() {
        return eventRepository.findByEventDate(LocalDate.now());
    }

    public List<Event> getCustomerEvents(String customerId) {
        return eventRepository.findByCustomerId(customerId);
    }

    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }

    @Transactional
    public Event createEventFromOrder(Order order) {
        // Check if event already exists for this order
        List<Event> existingEvents = eventRepository.findByOrderId(order.getId());
        if (!existingEvents.isEmpty()) {
            // Event already exists, return the first one
            return existingEvents.get(0);
        }

        // Get event details from the order
        EventDetails eventDetails = order.getEventDetails();
        if (eventDetails == null) {
            throw new RuntimeException("Event details not found for order: " + order.getId());
        }

        // Parse event date and time
        LocalDate eventDate;
        LocalTime eventTime;

        try {
            eventDate = LocalDate.parse(eventDetails.getEventDate());
        } catch (DateTimeParseException e) {
            // Fallback to current date + 7 days if date format is invalid
            eventDate = LocalDate.now().plusDays(7);
        }

        try {
            if (eventDetails.getEventTime() != null && !eventDetails.getEventTime().isEmpty()) {
                eventTime = LocalTime.parse(eventDetails.getEventTime());
            } else {
                // Default to 9 AM if no time specified
                eventTime = LocalTime.of(9, 0);
            }
        } catch (DateTimeParseException e) {
            // Fallback to 9 AM if time format is invalid
            eventTime = LocalTime.of(9, 0);
        }

        // Create new event
        Event event = new Event();
        event.setTitle("Order #" + order.getOrderNumber());
        event.setDescription("Event for " + eventDetails.getCustomName() + " at " + eventDetails.getVenue());
        event.setEventDate(eventDate);
        event.setEventTime(eventTime);
        event.setLocation(eventDetails.getVenue());
        event.setOrderId(order.getId());
        event.setCustomerId(order.getCustomer().getcustomerId().toString());
        event.setStatus("scheduled");

        Event savedEvent = eventRepository.save(event);

        // Notify admin about new event
        notificationService.createEventNotification(order, savedEvent);

        return savedEvent;
    }

    @Transactional
    public Event updateEvent(Long eventId, Event eventDetails) {
        Event event = getEventById(eventId);

        if (eventDetails.getTitle() != null) {
            event.setTitle(eventDetails.getTitle());
        }
        if (eventDetails.getDescription() != null) {
            event.setDescription(eventDetails.getDescription());
        }
        if (eventDetails.getEventDate() != null) {
            event.setEventDate(eventDetails.getEventDate());
        }
        if (eventDetails.getEventTime() != null) {
            event.setEventTime(eventDetails.getEventTime());
        }
        if (eventDetails.getLocation() != null) {
            event.setLocation(eventDetails.getLocation());
        }

        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEventStatus(Long eventId, String status) {
        Event event = getEventById(eventId);

        // Validate status
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        event.setStatus(status);

        // If event is completed, update the corresponding order
        if ("completed".equals(status)) {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + event.getOrderId()));

            order.setStatus("completed");
            orderRepository.save(order);
        }

        return eventRepository.save(event);
    }

    private boolean isValidStatus(String status) {
        return "scheduled".equals(status) ||
                "in-progress".equals(status) ||
                "completed".equals(status) ||
                "cancelled".equals(status);
    }

    // Get count of today's events for admin dashboard
    public long getTodayEventsCount() {
        return eventRepository.countByEventDate(LocalDate.now());
    }

    // Get upcoming events
    public List<Event> getUpcomingEvents() {
        return eventRepository.findByEventDateGreaterThanEqualOrderByEventDate(LocalDate.now());
    }
}
