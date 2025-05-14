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

    // NEW: Added validation methods
    private void validateCustomerData(Customer customer) {
        // Trim all string fields to handle spaces
        customer.setFirstName(customer.getFirstName().trim());
        customer.setLastName(customer.getLastName().trim());
        customer.setEmail(customer.getEmail().trim());
        customer.setUsername(customer.getUsername().trim());
        customer.setContact(customer.getContact().trim());
        // Don't trim password as spaces might be intentional

        // NEW: Convert email to lowercase
        customer.setEmail(customer.getEmail().toLowerCase());

        // Validate all fields are filled (additional check)
        if (customer.getFirstName().isEmpty() || customer.getLastName().isEmpty() ||
                customer.getEmail().isEmpty() || customer.getUsername().isEmpty() ||
                customer.getContact().isEmpty() || customer.getPassword().isEmpty()) {
            throw new IllegalArgumentException("All fields must be filled");
        }

        // Validate username is not same as password (case-insensitive)
        if (customer.getUsername().equalsIgnoreCase(customer.getPassword())) {
            throw new IllegalArgumentException("Username cannot be the same as password");
        }

        // Validate password has at least 8 characters
        if (customer.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Validate email format (basic regex)
        String emailRegex = "^[a-z0-9+_.-]+@[a-z0-9.-]+\\.[a-z]{2,}$";
        if (!customer.getEmail().matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Validate phone number is exactly 10 digits
        String phoneRegex = "^[0-9]{10}$";
        if (!customer.getContact().matches(phoneRegex)) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits");
        }

        // NEW: Check for existing email and phone
        Optional<Customer> existingCustomerByEmail = customerRepository.findByEmail(customer.getEmail());
        if (existingCustomerByEmail.isPresent() &&
                (customer.getcustomerId() == null || !existingCustomerByEmail.get().getcustomerId().equals(customer.getcustomerId()))) {
            throw new IllegalArgumentException("Email already exists");
        }

        Optional<Customer> existingCustomerByPhone = customerRepository.findByContact(customer.getContact());
        if (existingCustomerByPhone.isPresent() &&
                (customer.getcustomerId() == null || !existingCustomerByPhone.get().getcustomerId().equals(customer.getcustomerId()))) {
            throw new IllegalArgumentException("Phone number already exists");
        }
    }

    // MODIFIED: Added validation before saving
    public Customer createCustomer(Customer customer) {
        // NEW: Validate customer data
        validateCustomerData(customer);

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

    // MODIFIED: Added validation for updates
    public Customer updateCustomer(Customer customer) {
        // Validate only the fields that can be updated
        customer.setFirstName(customer.getFirstName().trim());
        customer.setLastName(customer.getLastName().trim());
        customer.setEmail(customer.getEmail().trim().toLowerCase());
        customer.setContact(customer.getContact().trim());

        // Check all fields are filled
        if (customer.getFirstName().isEmpty() || customer.getLastName().isEmpty() ||
                customer.getEmail().isEmpty() || customer.getContact().isEmpty()) {
            throw new IllegalArgumentException("All fields must be filled");
        }

        // Validate email and phone for updates
        String emailRegex = "^[a-z0-9+_.-]+@[a-z0-9.-]+\\.[a-z]{2,}$";
        if (!customer.getEmail().matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        String phoneRegex = "^[0-9]{10}$";
        if (!customer.getContact().matches(phoneRegex)) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits");
        }

        // Check for duplicate email/phone
        Optional<Customer> existingByEmail = customerRepository.findByEmail(customer.getEmail());
        if (existingByEmail.isPresent() && !existingByEmail.get().getcustomerId().equals(customer.getcustomerId())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Optional<Customer> existingByPhone = customerRepository.findByContact(customer.getContact());
        if (existingByPhone.isPresent() && !existingByPhone.get().getcustomerId().equals(customer.getcustomerId())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

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

        // NEW: Validate new password
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        // NEW: Ensure new password is not same as username
        if (username.equalsIgnoreCase(newPassword)) {
            throw new IllegalArgumentException("Password cannot be the same as username");
        }

        // Update password
        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);

        return true;
    }
}