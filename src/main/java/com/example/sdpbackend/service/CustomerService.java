package com.example.sdpbackend.service;

import com.example.sdpbackend.entity.Customer;
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

    public Customer createCustomer(Customer customer) {
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        return customerRepository.save(customer);
    }

    public Customer findByUsername (String username){
            System.out.println("Querying database for user: " + username);
            return customerRepository.findByUsername(username); // Directly returns null if not found
        }

    public Customer findById(int id) {
        Optional<Customer> customer = customerRepository.findById(id);
        return customer.orElse(null);
    }

    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Add the missing changePassword method
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        Customer customer = findByUsername(username);

        if (customer == null) {
            return false;
        }

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
