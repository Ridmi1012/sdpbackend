package com.example.sdpbackend.controller;

import com.example.sdpbackend.dto.ForgotPasswordRequest;
import com.example.sdpbackend.dto.PasswordChangeRequest;
import com.example.sdpbackend.dto.ResetPasswordRequest;
import com.example.sdpbackend.service.CustomerService;
import com.example.sdpbackend.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/password")
@CrossOrigin(origins = "http://localhost:4200")
public class PasswordController {
    private final CustomerService customerService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public PasswordController(CustomerService customerService, PasswordResetService passwordResetService) {

        this.customerService = customerService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/change")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request) {
        System.out.println("Password change request received for user: " + request.getUsername());

        try {
            boolean success = customerService.changePassword(
                    request.getUsername(),
                    request.getCurrentPassword(),
                    request.getNewPassword()
            );

            if (success) {
                return ResponseEntity.ok().body(Map.of("message", "Password updated successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Current password is incorrect"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error changing password: " + e.getMessage()));
        }
    }

    /**
     * NEW METHOD - Handle forgot password request
     * Generates a reset token for the given username
     */
    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        System.out.println("Forgot password request received for user: " + request.getUsername());

        try {
            String token = passwordResetService.generatePasswordResetToken(request.getUsername());

            if (token != null) {
                // In a real application, you would send this token via email
                // For development/testing, we're returning it in the response
                return ResponseEntity.ok().body(Map.of(
                        "message", "Password reset token generated successfully",
                        "token", token // In production, don't return the token - send it via email
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error processing forgot password request: " + e.getMessage()));
        }
    }

    /**
     * NEW METHOD - Handle password reset with token
     * Resets the password using the provided token
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        System.out.println("Password reset request received with token");

        try {
            boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

            if (success) {
                return ResponseEntity.ok().body(Map.of("message", "Password reset successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error resetting password: " + e.getMessage()));
        }
    }

    /**
     * NEW METHOD - Validate reset token
     * Checks if a reset token is valid
     */
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        System.out.println("Token validation request received");

        try {
            boolean isValid = passwordResetService.validateToken(token);

            return ResponseEntity.ok().body(Map.of("isValid", isValid));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error validating token: " + e.getMessage()));
        }
    }
}
