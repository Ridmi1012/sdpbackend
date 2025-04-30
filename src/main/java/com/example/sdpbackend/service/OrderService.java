package com.example.sdpbackend.service;



import com.example.sdpbackend.dto.CustomizedOrderDTO;
import com.example.sdpbackend.dto.FullyCustomOrderDTO;
import com.example.sdpbackend.dto.OrderAsIsDTO;
import com.example.sdpbackend.dto.OrderResponseDTO;
import com.example.sdpbackend.entity.*;
import com.example.sdpbackend.exception.ResourceNotFoundException;
import com.example.sdpbackend.repository.ItemRepository;
import com.example.sdpbackend.repository.OrderItemRepository;
import com.example.sdpbackend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DesignService designService;
    private final CustomerService customerService;
    private final ItemRepository itemRepository;
    private final CloudinaryService cloudinaryService;

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            DesignService designService,
            CustomerService customerService,
            ItemRepository itemRepository,
            CloudinaryService cloudinaryService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.designService = designService;
        this.customerService = customerService;
        this.itemRepository = itemRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public OrderResponseDTO createOrderAsIs(OrderAsIsDTO orderAsIsDTO) {
        // Get design
        Integer designId = orderAsIsDTO.getDesignId();
        Design design = designService.getDesignEntityById(designId);
        if (design == null) {
            throw new ResourceNotFoundException("Design not found with id: " + orderAsIsDTO.getDesignId());
        }

        // Get customer
        Integer customerId = orderAsIsDTO.getCustomerId();
        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            throw new ResourceNotFoundException("Customer not found with id: " + orderAsIsDTO.getCustomerId());
        }

        // Create the order
        Order order = new Order();
        order.setCustomer(customer);
        order.setDesign(design);
        order.setOrderType(OrderType.AS_IS);
        order.setOrderStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        order.setTotalPrice(design.getBasePrice().doubleValue()); // Using base price for "as-is" orders
        order.setDeliveryAddress(orderAsIsDTO.getDeliveryAddress());
        // Handle date conversion: LocalDate to LocalDateTime
        order.setEventDate(orderAsIsDTO.getEventDate().atStartOfDay());
        order.setCustomName(orderAsIsDTO.getCustomName());
        order.setCustomAge(orderAsIsDTO.getCustomAge());

        Order savedOrder = orderRepository.save(order);

        // Create order items from design items
        List<DesignItem> designItems = design.getItems();
        if (designItems != null) {
            for (DesignItem designItem : designItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setItem(designItem.getItem());
                orderItem.setQuantity(designItem.getDefaultQuantity());
                orderItem.setIsAddOn(false);

                orderItemRepository.save(orderItem);
            }
        }

        // Create response DTO
        return mapToOrderResponseDTO(savedOrder);
    }

    private OrderResponseDTO mapToOrderResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setOrderType(order.getOrderType().toString());
        dto.setOrderDate(order.getOrderDate());
        dto.setEventDate(order.getEventDate());
        if (order.getDesign() != null) {
            dto.setDesignName(order.getDesign().getName());
        } else if (order.getThemeName() != null) {
            // For fully custom orders
            dto.setDesignName("Custom: " + order.getThemeName());
        }
        return dto;
    }

    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return mapToOrderResponseDTO(order);
    }

    public List<OrderResponseDTO> getOrdersByCustomer(Integer customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream()
                .map(this::mapToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getOrdersByType(String orderType) {
        OrderType type;
        try {
            type = OrderType.valueOf(orderType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order type: " + orderType);
        }

        List<Order> orders = orderRepository.findByOrderType(type);
        return orders.stream()
                .map(this::mapToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getOrdersByStatus(String status) {
        List<Order> orders = orderRepository.findByOrderStatus(status.toUpperCase());
        return orders.stream()
                .map(this::mapToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    public OrderResponseDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Validate status
        if (!isValidStatus(newStatus)) {
            throw new IllegalArgumentException("Invalid order status: " + newStatus);
        }

        order.setOrderStatus(newStatus.toUpperCase());
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderResponseDTO(updatedOrder);
    }

    /**
     * Get all orders (for admin use)
     * @return List of all orders
     */
    public List<OrderResponseDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::mapToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    private boolean isValidStatus(String status) {
        String upperStatus = status.toUpperCase();
        return upperStatus.equals("PENDING") ||
                upperStatus.equals("CONFIRMED") ||
                upperStatus.equals("IN_PROGRESS") ||
                upperStatus.equals("COMPLETED") ||
                upperStatus.equals("CANCELLED");
    }

    @Transactional
    public OrderResponseDTO createFullyCustomOrder(FullyCustomOrderDTO fullyCustomOrderDTO) {
        // Get customer
        Integer customerId = fullyCustomOrderDTO.getCustomerId();
        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }

        // Create the order
        Order order = new Order();
        order.setCustomer(customer);
        // No design reference for fully custom orders
        order.setOrderType(OrderType.FULLY_CUSTOM);
        order.setOrderStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        order.setDeliveryAddress(fullyCustomOrderDTO.getDeliveryAddress());

        // Handle date conversion
        order.setEventDate(fullyCustomOrderDTO.getEventDate().atStartOfDay());

        // Store customization details
        order.setCustomName(fullyCustomOrderDTO.getCustomName());
        order.setCustomAge(fullyCustomOrderDTO.getCustomAge());
        order.setThemeColor(fullyCustomOrderDTO.getThemeColor());
        order.setThemeName(fullyCustomOrderDTO.getThemeName());
        order.setConceptDescription(fullyCustomOrderDTO.getConceptDescription());

        // Store inspiration images if provided
        if (fullyCustomOrderDTO.getInspirationImageUrls() != null && !fullyCustomOrderDTO.getInspirationImageUrls().isEmpty()) {
            order.setInspirationImages(fullyCustomOrderDTO.getInspirationImageUrls());
        }

        // Calculate starting price - for fully custom orders, we'll use a base price estimation
        // This should be adjusted based on business logic
        double totalPrice = 25.0; // Starting base price for custom orders

        Order savedOrder = orderRepository.save(order);

        // Add requested items
        if (fullyCustomOrderDTO.getItems() != null) {
            for (FullyCustomOrderDTO.CustomItemDTO itemDTO : fullyCustomOrderDTO.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);

                // Check if this is an existing catalog item or a custom item
                if (itemDTO.getItemId() != null) {
                    // Existing catalog item
                    Item item = itemRepository.findById(itemDTO.getItemId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Item not found with id: " + itemDTO.getItemId()));

                    orderItem.setItem(item);
                    // Add item price to total
                    totalPrice += item.getUnitPrice().doubleValue() * itemDTO.getQuantity();
                } else {
                    // Custom item (not in catalog)
                    orderItem.setCustomItemName(itemDTO.getCustomItemName());
                    orderItem.setCustomItemDescription(itemDTO.getDescription());
                    // For custom items, add a standard charge
                    totalPrice += 10.0 * itemDTO.getQuantity(); // Example pricing
                }

                orderItem.setQuantity(itemDTO.getQuantity());
                orderItemRepository.save(orderItem);
            }
        }

        // Update the total price
        savedOrder.setTotalPrice(totalPrice);
        savedOrder = orderRepository.save(savedOrder);

        return mapToOrderResponseDTO(savedOrder);
    }

    @Transactional
    public OrderResponseDTO createCustomizedOrder(CustomizedOrderDTO customizedOrderDTO) {
        // Get base design
        Integer baseDesignId = customizedOrderDTO.getBaseDesignId();
        Design baseDesign = designService.getDesignEntityById(baseDesignId);
        if (baseDesign == null) {
            throw new ResourceNotFoundException("Design not found with id: " + baseDesignId);
        }

        // Get customer
        Integer customerId = customizedOrderDTO.getCustomerId();
        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }

        // Create the order
        Order order = new Order();
        order.setCustomer(customer);
        order.setDesign(baseDesign); // Reference to the original design
        order.setOrderType(OrderType.CUSTOMIZED);
        order.setOrderStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        order.setDeliveryAddress(customizedOrderDTO.getDeliveryAddress());

        // Handle date conversion
        order.setEventDate(customizedOrderDTO.getEventDate().atStartOfDay());

        // Store customization details
        order.setCustomName(customizedOrderDTO.getCustomName());
        order.setCustomAge(customizedOrderDTO.getCustomAge());
        order.setThemeColor(customizedOrderDTO.getThemeColor());
        order.setConceptDescription(customizedOrderDTO.getConceptDescription());

        // First, get all items from the base design and convert to a map for easy lookup
        Map<Integer, DesignItem> designItemMap = baseDesign.getItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getItem().getItemID(),
                        Function.identity()
                ));

        // Create a set of item IDs to be removed
        List<Integer> removeItemIds = customizedOrderDTO.getRemoveItems() != null ?
                customizedOrderDTO.getRemoveItems().stream()
                        .map(CustomizedOrderDTO.OrderItemDTO::getItemId)
                        .collect(Collectors.toList()) :
                new ArrayList<>();

        // Create a map of item IDs to be modified
        Map<Integer, Integer> modifyItemsMap = customizedOrderDTO.getModifyItems() != null ?
                customizedOrderDTO.getModifyItems().stream()
                        .collect(Collectors.toMap(
                                CustomizedOrderDTO.OrderItemDTO::getItemId,
                                CustomizedOrderDTO.OrderItemDTO::getQuantity
                        )) :
                Map.of();

        // Calculate total price, starting with base price
        double totalPrice = baseDesign.getBasePrice().doubleValue();

        Order savedOrder = orderRepository.save(order);

        // Add items from the base design, excluding the ones to be removed
        // and adjusting quantities for the ones to be modified
        for (DesignItem designItem : baseDesign.getItems()) {
            Integer itemId = designItem.getItem().getItemID();

            // Skip removed items
            if (removeItemIds.contains(itemId)) {
                continue;
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setItem(designItem.getItem());

            // Use modified quantity if specified, otherwise use default
            int quantity = modifyItemsMap.containsKey(itemId) ?
                    modifyItemsMap.get(itemId) : designItem.getDefaultQuantity();
            orderItem.setQuantity(quantity);
            orderItem.setIsAddOn(false);

            orderItemRepository.save(orderItem);

            // Add item price to total
            totalPrice += designItem.getItem().getUnitPrice().doubleValue() * quantity;
        }

        // Add new items
        if (customizedOrderDTO.getAddItems() != null) {
            for (CustomizedOrderDTO.OrderItemDTO addItemDTO : customizedOrderDTO.getAddItems()) {
                Item item = itemRepository.findById(addItemDTO.getItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + addItemDTO.getItemId()));

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setItem(item);
                orderItem.setQuantity(addItemDTO.getQuantity());
                orderItem.setIsAddOn(true); // Mark as add-on

                orderItemRepository.save(orderItem);

                // Add item price to total
                totalPrice += item.getUnitPrice().doubleValue() * addItemDTO.getQuantity();
            }
        }

        // Update the total price
        savedOrder.setTotalPrice(totalPrice);
        savedOrder = orderRepository.save(savedOrder);

        return mapToOrderResponseDTO(savedOrder);
    }

    /**
     * Converts a list of Order entities to OrderResponseDTOs
     * @param orders List of Order entities
     * @return List of OrderResponseDTOs
     */
    public List<OrderResponseDTO> convertToOrderResponseDTOList(List<Order> orders) {
        return orders.stream()
                .map(this::mapToOrderResponseDTO)
                .collect(Collectors.toList());
    }
}
