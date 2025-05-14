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
import java.util.Random;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final CustomerService customerService;
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;
    // NEW: Added email service
    private final EmailService emailService;

    @Autowired
    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                CustomerService customerService,
                                AdminService adminService,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.customerService = customerService;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * CHANGED: Now generates a verification code and sends it via email
     * @param username The username requesting password reset
     * @return true if code was sent successfully, false if user not found
     */
    public boolean generateAndSendResetCode(String username) {
        // Check if user is a customer
        Optional<Customer> customer = customerService.findByUsername(username);
        if (customer.isPresent()) {
            String email = customer.get().getEmail();
            if (email != null && !email.isEmpty()) {
                sendResetCode(username, email, "CUSTOMER");
                return true;
            }
        }

        // Check if user is an admin
        Admin admin = adminService.findByUsername(username);
        if (admin != null && admin.getEmail() != null && !admin.getEmail().isEmpty()) {
            sendResetCode(username, admin.getEmail(), "ADMIN");
            return true;
        }

        return false; // User not found or email not available
    }

    /**
     * NEW METHOD - Generate and send verification code
     */
    private void sendResetCode(String username, String email, String userType) {
        // Delete any existing token for this user
        Optional<PasswordResetToken> existingToken = tokenRepository.findByUsernameAndUserType(username, userType);
        existingToken.ifPresent(token -> tokenRepository.delete(token));

        // Generate 6-digit verification code
        String verificationCode = generateVerificationCode();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .verificationCode(verificationCode)
                .username(username)
                .email(email)
                .userType(userType)
                .expiryDate(LocalDateTime.now().plusMinutes(10)) // Code expires in 10 minutes
                .used(false)
                .attemptCount(0)
                .build();

        tokenRepository.save(resetToken);

        // Send code via email
        emailService.sendPasswordResetCode(email, verificationCode, username);
    }

    /**
     * NEW METHOD - Generate random 6-digit verification code
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Generates 6-digit code
        return String.valueOf(code);
    }

    /**
     * NEW METHOD - Verify the code entered by user
     * @param username The username
     * @param code The verification code entered
     * @return true if code is valid, false otherwise
     */
    public boolean verifyResetCode(String username, String code) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUsernameAndVerificationCode(username, code);

        if (tokenOpt.isEmpty()) {
            // Try to increment attempt count even if code doesn't match
            Optional<PasswordResetToken> userTokenOpt = tokenRepository.findFirstByUsernameOrderByIdDesc(username);
            if (userTokenOpt.isPresent()) {
                PasswordResetToken userToken = userTokenOpt.get();
                userToken.setAttemptCount(userToken.getAttemptCount() + 1);
                tokenRepository.save(userToken);
            }
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Check various conditions
        if (resetToken.isExpired() || resetToken.isUsed() || resetToken.isMaxAttemptsExceeded()) {
            return false;
        }

        // Mark as verified (but not used yet - that happens after password is actually reset)
        return true;
    }

    /**
     * CHANGED: Now requires username and verification code instead of token
     * @param username The username
     * @param code The verification code
     * @param newPassword The new password
     * @return true if successful, false otherwise
     */
    public boolean resetPasswordWithCode(String username, String code, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUsernameAndVerificationCode(username, code);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Validate token
        if (resetToken.isExpired() || resetToken.isUsed() || resetToken.isMaxAttemptsExceeded()) {
            return false;
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
     * Update customer password without requiring current password
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
     * Update admin password without requiring current password
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
}
