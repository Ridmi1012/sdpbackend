package com.example.sdpbackend.service;


import com.example.sdpbackend.entity.Admin;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.PasswordResetToken;
import com.example.sdpbackend.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {


    private final PasswordResetTokenRepository tokenRepository;
    private final CustomerService customerService;
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                CustomerService customerService,
                                AdminService adminService,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.customerService = customerService;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * NEW METHOD - Generate password reset token for a user
     * @param username The username requesting password reset
     * @return The generated token or null if user not found
     */
    public String generatePasswordResetToken(String username) {
        // Check if user is a customer
        Optional<Customer> customer = customerService.findByUsername(username);
        if (customer.isPresent()) {
            return createResetToken(username, "CUSTOMER");
        }

        // Check if user is an admin
        Admin admin = adminService.findByUsername(username);
        if (admin != null) {
            return createResetToken(username, "ADMIN");
        }

        return null; // User not found
    }

    /**
     * NEW METHOD - Create and save reset token
     */
    private String createResetToken(String username, String userType) {
        // Delete any existing token for this user
        Optional<PasswordResetToken> existingToken = tokenRepository.findByUsernameAndUserType(username, userType);
        existingToken.ifPresent(token -> tokenRepository.delete(token));

        // Generate new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .username(username)
                .userType(userType)
                .expiryDate(LocalDateTime.now().plusHours(1)) // Token expires in 1 hour
                .used(false)
                .build();

        tokenRepository.save(resetToken);
        return token;
    }

    /**
     * NEW METHOD - Reset password using token
     * @param token The reset token
     * @param newPassword The new password
     * @return true if successful, false otherwise
     */
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(token);

        if (resetTokenOpt.isEmpty()) {
            return false; // Token not found
        }

        PasswordResetToken resetToken = resetTokenOpt.get();

        // Validate token
        if (resetToken.isExpired() || resetToken.isUsed()) {
            return false; // Token expired or already used
        }

        // Update password based on user type
        boolean success = false;
        if ("CUSTOMER".equals(resetToken.getUserType())) {
            success = updateCustomerPassword(resetToken.getUsername(), newPassword);
        } else if ("ADMIN".equals(resetToken.getUserType())) {
            success = updateAdminPassword(resetToken.getUsername(), newPassword);
        }

        if (success) {
            // Mark token as used
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);
        }

        return success;
    }

    /**
     * NEW METHOD - Update customer password without requiring current password
     */
    private boolean updateCustomerPassword(String username, String newPassword) {
        Optional<Customer> customerOpt = customerService.findByUsername(username);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setPassword(passwordEncoder.encode(newPassword));
            customerService.updateCustomer(customer);
            return true;
        }
        return false;
    }

    /**
     * NEW METHOD - Update admin password without requiring current password
     */
    private boolean updateAdminPassword(String username, String newPassword) {
        Admin admin = adminService.findByUsername(username);
        if (admin != null) {
            admin.setPassword(passwordEncoder.encode(newPassword));
            adminService.updateAdmin(admin);
            return true;
        }
        return false;
    }

    /**
     * NEW METHOD - Validate reset token
     * @param token The token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(token);

        if (resetTokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = resetTokenOpt.get();
        return !resetToken.isExpired() && !resetToken.isUsed();
    }
}
