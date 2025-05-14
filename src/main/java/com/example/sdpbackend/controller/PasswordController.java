package com.example.sdpbackend.controller;

import com.example.sdpbackend.dto.*;
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
     * CHANGED: Now sends verification code via email instead of generating token
     */
    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        System.out.println("Forgot password request received for user: " + request.getUsername());

        try {
            boolean success = passwordResetService.generateAndSendResetCode(request.getUsername());

            if (success) {
                return ResponseEntity.ok().body(Map.of(
                        "message", "Verification code has been sent to your email"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found or email not available"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error processing forgot password request: " + e.getMessage()));
        }
    }

    /**
     * NEW METHOD - Verify the code sent to email
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequest request) {
        System.out.println("Code verification request received for user: " + request.getUsername());

        try {
            boolean isValid = passwordResetService.verifyResetCode(
                    request.getUsername(),
                    request.getCode()
            );

            if (isValid) {
                return ResponseEntity.ok().body(Map.of("isValid", true));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid or expired code"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error verifying code: " + e.getMessage()));
        }
    }

    /**
     * CHANGED: Now requires username and code instead of just token
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody CodeResetPasswordRequest request) {
        System.out.println("Password reset request received for user: " + request.getUsername());

        try {
            boolean success = passwordResetService.resetPasswordWithCode(
                    request.getUsername(),
                    request.getCode(),
                    request.getNewPassword()
            );

            if (success) {
                return ResponseEntity.ok().body(Map.of("message", "Password reset successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid or expired code"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error resetting password: " + e.getMessage()));
        }
    }

}

