package com.example.sdpbackend.controller;


import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;


@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "http://localhost:4200")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // MODIFIED: Added exception handling for validation errors
    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody Customer customer) {
        try {
            Customer createdCustomer = customerService.createCustomer(customer);
            return ResponseEntity.ok(createdCustomer);
        } catch (IllegalArgumentException e) {
            // NEW: Return validation errors with proper status
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable int id) {
        Customer customer = customerService.findById(id);
        if (customer != null) {
            return ResponseEntity.ok(customer);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // MODIFIED: Added exception handling for validation errors
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable int id, @RequestBody Customer updatedCustomer) {
        if (id != updatedCustomer.getcustomerId()) {
            return ResponseEntity.badRequest().build();
        }

        Customer existingCustomer = customerService.findById(id);
        if (existingCustomer == null) {
            return ResponseEntity.notFound().build();
        }

        // Ensure we don't update username or password through this endpoint
        updatedCustomer.setUsername(existingCustomer.getUsername());
        updatedCustomer.setPassword(existingCustomer.getPassword());

        try {
            Customer savedCustomer = customerService.updateCustomer(updatedCustomer);
            return ResponseEntity.ok(savedCustomer);
        } catch (IllegalArgumentException e) {
            // NEW: Return validation errors with proper status
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
