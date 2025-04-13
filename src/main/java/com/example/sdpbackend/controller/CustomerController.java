package com.example.sdpbackend.controller;

import com.example.sdpbackend.dto.PasswordChangeRequest;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "http://localhost:4200")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerService.createCustomer(customer);
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

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable int id, @RequestBody Customer updatedCustomer) {
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

        Customer savedCustomer = customerService.updateCustomer(updatedCustomer);
        return ResponseEntity.ok(savedCustomer);
    }
}
