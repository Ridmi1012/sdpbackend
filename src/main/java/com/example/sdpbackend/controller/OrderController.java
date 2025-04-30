package com.example.sdpbackend.controller;


import com.example.sdpbackend.dto.CustomizedOrderDTO;
import com.example.sdpbackend.dto.FullyCustomOrderDTO;
import com.example.sdpbackend.dto.OrderAsIsDTO;
import com.example.sdpbackend.dto.OrderResponseDTO;
import com.example.sdpbackend.entity.Order;
import com.example.sdpbackend.repository.OrderRepository;
import com.example.sdpbackend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    // Scenario 1: Order as-is with name/age customization
    @PostMapping("/as-is")
    public ResponseEntity<OrderResponseDTO> createOrderAsIs(@RequestBody OrderAsIsDTO orderAsIsDTO) {
        OrderResponseDTO response = orderService.createOrderAsIs(orderAsIsDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Scenario 2: Customize existing design
    @PostMapping("/customize")
    public ResponseEntity<OrderResponseDTO> createCustomizedOrder(@RequestBody CustomizedOrderDTO customizedOrderDTO) {
        OrderResponseDTO response = orderService.createCustomizedOrder(customizedOrderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Scenario 3: Fully custom design
    @PostMapping("/fully-custom")
    public ResponseEntity<OrderResponseDTO> createFullyCustomOrder(@RequestBody FullyCustomOrderDTO fullyCustomOrderDTO) {
        OrderResponseDTO response = orderService.createFullyCustomOrder(fullyCustomOrderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get a specific order
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable Long orderId) {
        OrderResponseDTO order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    // Get all orders for a customer
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponseDTO>> getCustomerOrders(@PathVariable Integer customerId) {
        List<OrderResponseDTO> orders = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }

    // Get orders by type
    @GetMapping("/type/{orderType}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByType(@PathVariable String orderType) {
        List<OrderResponseDTO> orders = orderService.getOrdersByType(orderType);
        return ResponseEntity.ok(orders);
    }

    // Get orders by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByStatus(@PathVariable String status) {
        List<OrderResponseDTO> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    // Update order status - Admin only
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        OrderResponseDTO updatedOrder = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(updatedOrder);
    }

    // Get all orders - Added GetMapping for endpoint access
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orderService.convertToOrderResponseDTOList(orders));
    }
}
