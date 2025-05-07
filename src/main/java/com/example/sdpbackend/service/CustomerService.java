package com.example.sdpbackend.service;

import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.exception.ResourceNotFoundException;
import com.example.sdpbackend.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Changed from Long to Integer to match entity
    public Customer getCustomerById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    public Customer createCustomer(Customer customer) {
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        return customerRepository.save(customer);
    }

    public Optional<Customer> findByUsername(String username) {
        System.out.println("Querying database for user: " + username);
        return customerRepository.findByUsername(username); // Directly returns null if not found
    }

    public Customer findById(Integer id) {
        Optional<Customer> customer = customerRepository.findById(id);
        return customer.orElse(null);
    }

    // Add method to handle Long to Integer conversion if needed elsewhere
    public Customer findByLongId(Long id) {
        return findById(id.intValue());
    }

    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Fixed changePassword method to work with the Customer object directly
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        Optional<Customer> customerOpt = findByUsername(username);

        if (customerOpt.isEmpty()) {
            return false;
        }

        Customer customer = customerOpt.get();

        // Check if current password matches
        if (!passwordEncoder.matches(currentPassword, customer.getPassword())) {
            return false;
        }

        // Update password
        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);

        return true;
    }
}