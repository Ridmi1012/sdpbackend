package com.example.sdpbackend.service;

import com.example.sdpbackend.dto.OrderRequest;
import com.example.sdpbackend.dto.OrderResponse;
import com.example.sdpbackend.entity.*;
import com.example.sdpbackend.exception.OrderException;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.repository.DesignRepository;
import com.example.sdpbackend.repository.OrderItemRepository;
import com.example.sdpbackend.repository.OrderRepository;
import com.example.sdpbackend.util.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Autowired
    private OrderItemRepository orderItemRepository;

    private final Random random = new Random();

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, Customer customer) {
        Order order = new Order();

        // Set basic order properties
        order.setOrderNumber(generateOrderNumber());
        order.setDesignId(Long.valueOf(orderRequest.getDesignId()));
        order.setOrderType(orderRequest.getOrderType());
        order.setCustomer(customer);
        order.setStatus(orderRequest.getStatus() != null ? orderRequest.getStatus() : "pending");

        // Handle different order types
        if ("as-is".equals(order.getOrderType())) {
            // For as-is orders, get base price from design
            Long designId = Long.parseLong(String.valueOf(order.getDesignId()));
            Design design = designRepository.findById(Math.toIntExact(designId))
                    .orElseThrow(() -> new RuntimeException("Design not found with id: " + designId));
            order.setBasePrice(design.getBasePrice().doubleValue());
        } else if ("request-similar".equals(order.getOrderType())) {
            // For request-similar orders, set customization fields
            order.setThemeColor(orderRequest.getThemeColor());
            order.setConceptCustomization(orderRequest.getConceptCustomization());
            order.setBasePrice(0.0);
        } else if ("full-custom".equals(order.getOrderType())) {
            // NEW - For full-custom orders
            order.setThemeColor(orderRequest.getThemeColor());
            order.setConceptCustomization(orderRequest.getConceptCustomization());
            order.setSpecialNote(orderRequest.getSpecialNote());

            // Handle inspiration photos (max 3)
            if (orderRequest.getInspirationPhotos() != null) {
                List<String> photos = orderRequest.getInspirationPhotos().stream()
                        .limit(3) // Ensure max 3 photos
                        .collect(Collectors.toList());
                order.setInspirationPhotos(photos);
            }

            order.setBasePrice(0.0);
            order.setDesignId(0L); // No existing design for full-custom
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

        // Save the order first to get the ID
        Order savedOrder = orderRepository.save(order);

        // Handle order items for request-similar and full-custom orders
        if (("request-similar".equals(order.getOrderType()) || "full-custom".equals(order.getOrderType()))
                && orderRequest.getOrderItems() != null) {
            double totalItemsPrice = 0.0;
            List<OrderItem> orderItems = new ArrayList<>();

            for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setItemId(itemRequest.getItemId());
                orderItem.setItemName(itemRequest.getItemName());
                orderItem.setItemCategory(itemRequest.getItemCategory());
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setPricePerUnit(itemRequest.getPricePerUnit());
                orderItem.setTotalPrice(itemRequest.getQuantity() * itemRequest.getPricePerUnit());
                orderItem.setStatus(itemRequest.getStatus() != null ? itemRequest.getStatus() : "active");

                // Calculate total only for active items
                if ("active".equals(orderItem.getStatus())) {
                    totalItemsPrice += orderItem.getTotalPrice();
                }

                orderItems.add(orderItem);
            }

            // Save all order items
            orderItemRepository.saveAll(orderItems);
            savedOrder.setOrderItems(orderItems);

            // Update and save the base price
            savedOrder.setBasePrice(totalItemsPrice);
            savedOrder = orderRepository.save(savedOrder);
        }

        // Send notification to admin about new order
        notificationService.createOrderNotification(savedOrder);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderItems(Long orderId, List<OrderRequest.OrderItemRequest> itemRequests) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found with id: " + orderId));

        if (!"request-similar".equals(order.getOrderType()) && !"full-custom".equals(order.getOrderType())) {
            throw new OrderException("Order items can only be updated for request-similar or full-custom orders");
        }

        // Clear existing items
        orderItemRepository.deleteAll(order.getOrderItems());
        order.getOrderItems().clear();

        // Add new items
        double totalItemsPrice = 0.0;
        List<OrderItem> newOrderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : itemRequests) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItemId(itemRequest.getItemId());
            orderItem.setItemName(itemRequest.getItemName());
            orderItem.setItemCategory(itemRequest.getItemCategory());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPricePerUnit(itemRequest.getPricePerUnit());
            orderItem.setTotalPrice(itemRequest.getQuantity() * itemRequest.getPricePerUnit());
            orderItem.setStatus(itemRequest.getStatus() != null ? itemRequest.getStatus() : "active");

            if ("active".equals(orderItem.getStatus())) {
                totalItemsPrice += orderItem.getTotalPrice();
            }

            newOrderItems.add(orderItem);
        }

        orderItemRepository.saveAll(newOrderItems);
        order.setOrderItems(newOrderItems);

        // Update base price
        order.setBasePrice(totalItemsPrice);

        // Recalculate total price
        Double transportCost = order.getTransportationCost() != null ? order.getTransportationCost() : 0.0;
        Double additionalCost = order.getAdditionalRentalCost() != null ? order.getAdditionalRentalCost() : 0.0;
        order.setTotalPrice(totalItemsPrice + transportCost + additionalCost);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    // NEW METHOD - Update customization for request-similar orders
    @Transactional
    public OrderResponse updateCustomization(Long orderId, String themeColor, String conceptCustomization) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found with id: " + orderId));

        if (!"request-similar".equals(order.getOrderType()) && !"full-custom".equals(order.getOrderType())) {
            throw new OrderException("Customization can only be updated for request-similar or full-custom orders");
        }

        if (themeColor != null) {
            order.setThemeColor(themeColor);
        }
        if (conceptCustomization != null) {
            order.setConceptCustomization(conceptCustomization);
        }

        Order savedOrder = orderRepository.save(order);
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
        eventService.createOrUpdateEventFromOrder(savedOrder, true);

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
        LocalDateTime now = LocalDateTime.now();
        String datePrefix = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", random.nextInt(10000));
        return "ORD-" + datePrefix + "-" + randomSuffix;
    }

    // MODIFIED - Added support for request-similar orders
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

    // NEW METHOD - Update inspiration photos for full-custom orders
    @Transactional
    public OrderResponse updateInspirationPhotos(Long orderId, List<String> photoUrls) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found with id: " + orderId));

        if (!"full-custom".equals(order.getOrderType())) {
            throw new OrderException("Inspiration photos can only be updated for full-custom orders");
        }

        // Ensure max 3 photos
        List<String> limitedPhotos = photoUrls.stream()
                .limit(3)
                .collect(Collectors.toList());

        order.setInspirationPhotos(limitedPhotos);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    // NEW METHOD - Update special note for full-custom orders
    @Transactional
    public OrderResponse updateSpecialNote(Long orderId, String specialNote) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found with id: " + orderId));

        if (!"full-custom".equals(order.getOrderType())) {
            throw new OrderException("Special note can only be updated for full-custom orders");
        }

        order.setSpecialNote(specialNote);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    public boolean hasOrderForDesign(Integer customerId, Long designId) {
        List<String> activeStatuses = List.of("pending", "confirmed", "partial");
        List<Order> orders = orderRepository.findByCustomerCustomerIdAndDesignIdAndStatusIn(
                customerId, designId, activeStatuses);
        return !orders.isEmpty();
    }

    public List<OrderResponse> getOrdersForDesign(Integer customerId, Long designId) {
        List<String> activeStatuses = List.of("pending", "confirmed", "partial");
        List<Order> orders = orderRepository.findByCustomerCustomerIdAndDesignIdAndStatusIn(
                customerId, designId, activeStatuses);
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }


}


