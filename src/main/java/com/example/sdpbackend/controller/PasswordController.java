package com.example.sdpbackend.controller;

import com.example.sdpbackend.dto.PasswordChangeRequest;
import com.example.sdpbackend.service.CustomerService;
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

    @Autowired
    public PasswordController(CustomerService customerService) {
        this.customerService = customerService;
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
}
