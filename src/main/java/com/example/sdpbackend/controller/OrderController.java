package com.example.sdpbackend.controller;

import com.example.sdpbackend.dto.OrderRequest;
import com.example.sdpbackend.dto.OrderResponse;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.service.JWTService;
import com.example.sdpbackend.service.OrderService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JWTService jwtService;

    // Create a new order (customers only)
    // Create a new order (customers only)
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest orderRequest, HttpServletRequest request) {
        try {
            // Extract customer username from JWT token
            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Find the customer by username
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Customer not found"));
            }
            Customer customer = customerOpt.get();

            // Create the order with proper customer entity
            OrderResponse orderResponse = orderService.createOrder(orderRequest, customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to create order: " + e.getMessage()));
        }
    }

    @PutMapping("/{orderId}/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateOrderItems(@PathVariable Long orderId,
                                              @RequestBody List<OrderRequest.OrderItemRequest> itemRequests,
                                              HttpServletRequest request) {
        try {
            // Verify customer owns this order
            OrderResponse order = orderService.getOrderById(orderId);
            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().toString().equals(order.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only update your own orders"));
            }

            OrderResponse updatedOrder = orderService.updateOrderItems(orderId, itemRequests);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to update order items: " + e.getMessage()));
        }
    }

    // NEW - Update customization for request-similar and full-custom orders
    @PatchMapping("/{orderId}/customization")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateCustomization(@PathVariable Long orderId,
                                                 @RequestBody Map<String, String> customizationData,
                                                 HttpServletRequest request) {
        try {
            // Verify customer owns this order
            OrderResponse order = orderService.getOrderById(orderId);
            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().toString().equals(order.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only update your own orders"));
            }

            String themeColor = customizationData.get("themeColor");
            String conceptCustomization = customizationData.get("conceptCustomization");

            OrderResponse updatedOrder = orderService.updateCustomization(orderId, themeColor, conceptCustomization);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to update customization: " + e.getMessage()));
        }
    }

    @PatchMapping("/{orderId}/inspiration-photos")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateInspirationPhotos(@PathVariable Long orderId,
                                                     @RequestBody Map<String, List<String>> photosData,
                                                     HttpServletRequest request) {
        try {
            // Verify customer owns this order
            OrderResponse order = orderService.getOrderById(orderId);
            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().toString().equals(order.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only update your own orders"));
            }

            List<String> photoUrls = photosData.get("inspirationPhotos");
            OrderResponse updatedOrder = orderService.updateInspirationPhotos(orderId, photoUrls);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to update inspiration photos: " + e.getMessage()));
        }
    }

    // NEW - Update special note for full-custom orders
    @PatchMapping("/{orderId}/special-note")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateSpecialNote(@PathVariable Long orderId,
                                               @RequestBody Map<String, String> noteData,
                                               HttpServletRequest request) {
        try {
            // Verify customer owns this order
            OrderResponse order = orderService.getOrderById(orderId);
            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().toString().equals(order.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only update your own orders"));
            }

            String specialNote = noteData.get("specialNote");
            OrderResponse updatedOrder = orderService.updateSpecialNote(orderId, specialNote);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to update special note: " + e.getMessage()));
        }
    }

    // Get customer's orders
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getCustomerOrders(@PathVariable Integer customerId, HttpServletRequest request) {
        try {
            // If user is a customer, verify they can only access their own orders
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                // Get customer ID from username
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().equals(customerId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access your own orders"));
                }
            }

            List<OrderResponse> orders = orderService.getCustomerOrders(customerId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to fetch orders: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<OrderResponse> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to fetch orders: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            OrderResponse order = orderService.getOrderById(orderId);

            // If user is a customer, verify they can only access their own order
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                // Get customer from username
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().toString().equals(order.getCustomerId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only access your own orders"));
                }
            }

            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Order not found: " + e.getMessage()));
        }
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getNewOrders() {
        try {
            List<OrderResponse> orders = orderService.getNewOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to fetch new orders: " + e.getMessage()));
        }
    }

    // Get ongoing orders
    @GetMapping("/ongoing")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOngoingOrders(HttpServletRequest request) {
        try {
            List<OrderResponse> orders;

            if (request.isUserInRole("ROLE_CUSTOMER")) {
                // For customers, get only their ongoing orders
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                // Get customer ID from username
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Customer not found"));
                }
                Integer customerId = customerOpt.get().getcustomerId();
                orders = orderService.getCustomerOngoingOrders(customerId);
            } else {
                // For admin, get all ongoing orders
                orders = orderService.getOngoingOrders();
            }

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to fetch ongoing orders: " + e.getMessage()));
        }
    }

    // Confirm order (admin only)
    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId, @RequestBody Map<String, Double> additionalCosts) {
        try {
            Double transportationCost = additionalCosts.get("transportationCost");
            Double additionalRentalCost = additionalCosts.get("additionalRentalCost");

            OrderResponse order = orderService.confirmOrder(orderId, transportationCost, additionalRentalCost);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to confirm order: " + e.getMessage()));
        }
    }

    // Cancel order
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String reason = body.get("reason");

            // If user is a customer, verify they can only cancel their own order
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                OrderResponse order = orderService.getOrderById(orderId);
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                // Get customer from username
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().toString().equals(order.getCustomerId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only cancel your own orders"));
                }
            }

            OrderResponse order = orderService.cancelOrder(orderId, reason);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to cancel order: " + e.getMessage()));
        }
    }


    // Update event details (customer only)
    @PatchMapping("/{orderId}/event-details")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateEventDetails(@PathVariable Long orderId, @RequestBody Map<String, String> eventDetails, HttpServletRequest request) {
        try {
            // Verify customer owns this order
            OrderResponse order = orderService.getOrderById(orderId);
            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Get customer from username
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().toString().equals(order.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only update your own orders"));
            }

            String venue = eventDetails.get("venue");
            String eventDate = eventDetails.get("eventDate");
            String eventTime = eventDetails.get("eventTime");

            OrderResponse updatedOrder = orderService.updateEventDetails(orderId, venue, eventDate, eventTime);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to update event details: " + e.getMessage()));
        }
    }


    @PostMapping("/{orderId}/update")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateOrder(@PathVariable Long orderId, @RequestBody Map<String, Object> updates, HttpServletRequest request) {
        try {
            // Verify authorization (only owner or admin can update)
            OrderResponse currentOrder = orderService.getOrderById(orderId);
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                // Get customer from username
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent() || !customerOpt.get().getcustomerId().toString().equals(currentOrder.getCustomerId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only update your own orders"));
                }
            }

            // Handle different types of updates
            Double transportationCost = updates.get("transportationCost") != null ?
                    Double.valueOf(updates.get("transportationCost").toString()) : null;
            Double additionalRentalCost = updates.get("additionalRentalCost") != null ?
                    Double.valueOf(updates.get("additionalRentalCost").toString()) : null;
            String status = updates.get("status") != null ? updates.get("status").toString() : null;

            OrderResponse updatedOrder = orderService.updateOrder(orderId, transportationCost, additionalRentalCost, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update order: " + e.getMessage()));
        }
    }


    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("JWT Token is missing or invalid");
    }
}