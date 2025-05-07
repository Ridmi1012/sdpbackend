package com.example.sdpbackend.controller;


import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.Event;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.service.EventService;
import com.example.sdpbackend.service.JWTService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:4200")
public class EventController {
    @Autowired
    private EventService eventService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private CustomerRepository customerRepository;

    // Get all events (admin only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllEvents() {
        try {
            List<Event> events = eventService.getAllEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch events: " + e.getMessage()));
        }
    }

    // Get events for a specific date range
    @GetMapping("/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<Event> events = eventService.getEventsByDateRange(startDate, endDate);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch events: " + e.getMessage()));
        }
    }

    // Get today's events
    @GetMapping("/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTodayEvents() {
        try {
            List<Event> events = eventService.getTodayEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch today's events: " + e.getMessage()));
        }
    }

    // Get events for a customer
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getCustomerEvents(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // Get customer ID from username
            Optional<Customer> customerOpt = customerRepository.findByUsername(username);
            if (!customerOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Customer not found"));
            }

            String customerId = customerOpt.get().getcustomerId().toString();
            List<Event> events = eventService.getCustomerEvents(customerId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch customer events: " + e.getMessage()));
        }
    }

    // Get event by ID
    @GetMapping("/{eventId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getEventById(@PathVariable Long eventId, HttpServletRequest request) {
        try {
            Event event = eventService.getEventById(eventId);

            // If user is a customer, verify they can only access their own events
            if (request.isUserInRole("ROLE_CUSTOMER")) {
                String token = extractTokenFromRequest(request);
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                // Get customer ID from username
                Optional<Customer> customerOpt = customerRepository.findByUsername(username);
                if (!customerOpt.isPresent()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Customer not found"));
                }

                String customerId = customerOpt.get().getcustomerId().toString();

                if (!customerId.equals(event.getCustomerId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You are not authorized to view this event"));
                }
            }

            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch event: " + e.getMessage()));
        }
    }

    // Update event (admin only)
    @PutMapping("/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEvent(@PathVariable Long eventId, @RequestBody Event eventDetails) {
        try {
            Event updatedEvent = eventService.updateEvent(eventId, eventDetails);
            return ResponseEntity.ok(updatedEvent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update event: " + e.getMessage()));
        }
    }

    // Update event status (admin only)
    @PatchMapping("/{eventId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEventStatus(@PathVariable Long eventId, @RequestBody Map<String, String> statusUpdate) {
        try {
            String status = statusUpdate.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
            }

            Event updatedEvent = eventService.updateEventStatus(eventId, status);
            return ResponseEntity.ok(updatedEvent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update event status: " + e.getMessage()));
        }
    }

    // Get upcoming events
    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUpcomingEvents() {
        try {
            List<Event> events = eventService.getUpcomingEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch upcoming events: " + e.getMessage()));
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
