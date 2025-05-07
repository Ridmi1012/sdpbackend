package com.example.sdpbackend.service;

import com.example.sdpbackend.dto.OrderRequest;
import com.example.sdpbackend.dto.OrderResponse;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.Design;
import com.example.sdpbackend.entity.EventDetails;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.exception.OrderException;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.repository.DesignRepository;
import com.example.sdpbackend.repository.OrderRepository;
import com.example.sdpbackend.util.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DesignRepository designRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EventService eventService;

    private final Random random = new Random();

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, Customer customer) {
        // Create order entity using mapper
        Order order = new Order();

        // Set basic order properties
        order.setOrderNumber(generateOrderNumber());
        order.setDesignId(orderRequest.getDesignId());
        order.setOrderType(orderRequest.getOrderType());
        order.setCustomer(customer);
        order.setStatus(orderRequest.getStatus() != null ? orderRequest.getStatus() : "pending");

        // For as-is orders, fetch the base price from the design
        if ("as-is".equals(order.getOrderType())) {
            Long designId = Long.parseLong(order.getDesignId());
            Design design = designRepository.findById(Math.toIntExact(designId))
                    .orElseThrow(() -> new RuntimeException("Design not found with id: " + designId));
            order.setBasePrice(design.getBasePrice().doubleValue());
        }

        // Create and populate EventDetails
        EventDetails eventDetails = new EventDetails();
        eventDetails.setOrder(order);

        if (orderRequest.getCustomDetails() != null) {
            eventDetails.setCustomName(orderRequest.getCustomDetails().getCustomName());
            eventDetails.setCustomAge(orderRequest.getCustomDetails().getCustomAge());
            eventDetails.setVenue(orderRequest.getCustomDetails().getVenue());
            eventDetails.setEventDate(orderRequest.getCustomDetails().getEventDate());
            eventDetails.setEventTime(orderRequest.getCustomDetails().getEventTime());
            eventDetails.setEventCategory(orderRequest.getCustomDetails().getEventCategory());

            if (orderRequest.getCustomDetails().getRelationshipToPerson() != null) {
                eventDetails.setRelationshipToPerson(orderRequest.getCustomDetails().getRelationshipToPerson());
            } else if (orderRequest.getCustomerInfo() != null && orderRequest.getCustomerInfo().getRelationshipToPerson() != null) {
                eventDetails.setRelationshipToPerson(orderRequest.getCustomerInfo().getRelationshipToPerson());
            }
        }

        order.setEventDetails(eventDetails);

        // Save the order (cascade will save eventDetails)
        Order savedOrder = orderRepository.save(order);

        // Send notification to admin about new order
        notificationService.createOrderNotification(savedOrder);

        return orderMapper.toResponse(savedOrder);
    }

    public List<OrderResponse> getCustomerOrders(Integer customerId) {
        List<Order> orders = orderRepository.findByCustomerCustomerId(customerId);
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        return orderMapper.toResponse(order);
    }

    public List<OrderResponse> getNewOrders() {
        List<Order> orders = orderRepository.findByStatus("pending");
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOngoingOrders() {
        List<Order> orders = orderRepository.findByStatusIn(List.of("confirmed", "partial"));
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getCustomerOngoingOrders(Integer customerId) {
        List<Order> orders = orderRepository.findByCustomerCustomerIdAndStatusIn(
                customerId, List.of("confirmed", "partial"));
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse confirmOrder(Long orderId, Double transportationCost, Double additionalRentalCost) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus("confirmed");
        order.setTransportationCost(transportationCost);
        order.setAdditionalRentalCost(additionalRentalCost);

        // Calculate total price
        Double basePrice = order.getBasePrice() != null ? order.getBasePrice() : 0.0;
        Double totalPrice = basePrice + transportationCost + additionalRentalCost;
        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);

        // Create an event for this confirmed order
        eventService.createEventFromOrder(savedOrder);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus("cancelled");
        order.setCancellationReason(reason);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateEventDetails(Long orderId, String venue, String eventDate, String eventTime) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        EventDetails eventDetails = order.getEventDetails();
        if (eventDetails == null) {
            throw new RuntimeException("Event details not found for order with id: " + orderId);
        }

        if (venue != null) {
            eventDetails.setVenue(venue);
        }
        if (eventDate != null) {
            eventDetails.setEventDate(eventDate);
        }
        if (eventTime != null) {
            eventDetails.setEventTime(eventTime);
        }

        // The changes will be persisted due to CascadeType.ALL
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    private String generateOrderNumber() {
        // Generate a unique order number with format: ORD-YYYYMMDD-XXXX
        LocalDateTime now = LocalDateTime.now();
        String datePrefix = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", random.nextInt(10000));
        return "ORD-" + datePrefix + "-" + randomSuffix;
    }

    @Transactional
    public OrderResponse updateOrder(Long orderId, Double transportationCost, Double additionalRentalCost, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found with id: " + orderId));

        // Update fields if provided
        if (transportationCost != null) {
            order.setTransportationCost(transportationCost);
        }
        if (additionalRentalCost != null) {
            order.setAdditionalRentalCost(additionalRentalCost);
        }
        if (status != null) {
            order.setStatus(status);
        }

        // Recalculate total price if cost components were updated
        if (transportationCost != null || additionalRentalCost != null) {
            Double basePrice = order.getBasePrice() != null ? order.getBasePrice() : 0.0;
            Double newTransportCost = order.getTransportationCost() != null ? order.getTransportationCost() : 0.0;
            Double newAdditionalCost = order.getAdditionalRentalCost() != null ? order.getAdditionalRentalCost() : 0.0;
            Double totalPrice = basePrice + newTransportCost + newAdditionalCost;
            order.setTotalPrice(totalPrice);
        }

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }
}
