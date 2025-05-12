package com.example.sdpbackend.service;

import com.example.sdpbackend.entity.Event;
import com.example.sdpbackend.entity.EventDetails;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.repository.EventRepository;
import com.example.sdpbackend.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    // Define colors for different payment statuses
    private static final String COLOR_FULLY_PAID = "#4CAF50";  // Green
    private static final String COLOR_PARTIAL_PAID = "#FFC107"; // Amber
    private static final String COLOR_PENDING = "#F44336";      // Red
    private static final String COLOR_CANCELLED = "#9E9E9E";    // Grey

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create or update an event based on order information and payment status
     * @param order The order to create/update an event for
     * @param isFullyPaid Whether the order is fully paid
     */
    @Transactional
    public void createOrUpdateEventFromOrder(Order order, boolean isFullyPaid) {
        logger.info("Creating or updating event for order ID: {}, fully paid: {}", order.getId(), isFullyPaid);

        // Check if event already exists for this order
        List<Event> existingEvents = eventRepository.findByOrderId(order.getId());
        Event event;

        if (!existingEvents.isEmpty()) {
            // Update existing event
            event = existingEvents.get(0);
            logger.info("Updating existing event with ID: {}", event.getId());
        } else {
            // Create new event
            event = new Event();
            event.setOrderId(order.getId());
            event.setCustomerId(order.getCustomer().getcustomerId().toString());
            event.setStatus("scheduled");
            logger.info("Creating new event for order ID: {}", order.getId());
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
            logger.warn("Invalid event date format for order ID: {}, using fallback date", order.getId());
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
            logger.warn("Invalid event time format for order ID: {}, using fallback time", order.getId());
        }

        // Update event details
        String customerName = eventDetails.getCustomName();
        String venue = eventDetails.getVenue();

        // Set event details based on order info
        event.setTitle("Order #" + order.getOrderNumber() + (isFullyPaid ? "" : " (Payment Pending)"));
        event.setDescription("Event for " + customerName + " at " + venue +
                (isFullyPaid ? "" : " - PAYMENT PENDING"));
        event.setEventDate(eventDate);
        event.setEventTime(eventTime);
        event.setLocation(venue);

        // Set payment status and color
        event.setIsFullyPaid(isFullyPaid);

        if ("cancelled".equals(order.getStatus())) {
            event.setStatus("cancelled");
            event.setColor(COLOR_CANCELLED);
        } else if (isFullyPaid) {
            event.setColor(COLOR_FULLY_PAID);
        } else if ("partial".equals(order.getPaymentStatus())) {
            event.setColor(COLOR_PARTIAL_PAID);
        } else {
            event.setColor(COLOR_PENDING);
        }

        // Save the event
        Event savedEvent = eventRepository.save(event);
        logger.info("Event saved with ID: {}, payment status: {}, color: {}",
                savedEvent.getId(), isFullyPaid ? "PAID" : "PENDING", event.getColor());

        // Notify admin about event status (only for new events)
        if (existingEvents.isEmpty()) {
            try {
                notificationService.createEventNotification(order, savedEvent);
                logger.info("Event notification created for order ID: {}", order.getId());
            } catch (Exception e) {
                logger.error("Error creating event notification: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Get all events
     */
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * Get events by date range
     */
    public List<Event> getEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        return eventRepository.findByEventDateBetween(startDate, endDate);
    }

    /**
     * Get today's events
     */
    public List<Event> getTodayEvents() {
        return eventRepository.findByEventDate(LocalDate.now());
    }

    /**
     * Get customer's events
     */
    public List<Event> getCustomerEvents(String customerId) {
        return eventRepository.findByCustomerId(customerId);
    }

    /**
     * Get event by ID
     */
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }

    /**
     * Update an event
     */
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
        if (eventDetails.getStatus() != null) {
            event.setStatus(eventDetails.getStatus());
        }
        if (eventDetails.getColor() != null) {
            event.setColor(eventDetails.getColor());
        }
        if (eventDetails.getIsFullyPaid() != null) {
            event.setIsFullyPaid(eventDetails.getIsFullyPaid());
        }

        return eventRepository.save(event);
    }

    /**
     * Update event status
     */
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

    /**
     * Update events for all orders with payment status changes
     */
    @Transactional
    public void updateEventsBasedOnPaymentStatus() {
        logger.info("Updating events based on payment status");

        // Get all orders with confirmed status and events
        List<Order> ordersWithEvents = orderRepository.findByStatusAndHasEvents("confirmed");

        for (Order order : ordersWithEvents) {
            try {
                boolean isFullyPaid = "completed".equals(order.getPaymentStatus());
                createOrUpdateEventFromOrder(order, isFullyPaid);
            } catch (Exception e) {
                logger.error("Error updating event for order ID {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Check if status is valid
     */
    private boolean isValidStatus(String status) {
        return "scheduled".equals(status) ||
                "in-progress".equals(status) ||
                "completed".equals(status) ||
                "cancelled".equals(status);
    }

    /**
     * Get count of today's events
     */
    public long getTodayEventsCount() {
        return eventRepository.countByEventDate(LocalDate.now());
    }

    /**
     * Get upcoming events
     */
    public List<Event> getUpcomingEvents() {
        return eventRepository.findByEventDateGreaterThanEqualOrderByEventDate(LocalDate.now());
    }
}
