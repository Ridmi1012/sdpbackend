package com.example.sdpbackend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CHANGED: Now stores a 6-digit verification code instead of UUID token
    @Column(nullable = false)
    private String verificationCode;

    @Column(nullable = false)
    private String username;

    // NEW: Added email field to send the code
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String userType; // "CUSTOMER" or "ADMIN"

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    // NEW: Track verification attempts for security
    @Column(nullable = false)
    private int attemptCount = 0;

    // NEW: Maximum allowed attempts
    private static final int MAX_ATTEMPTS = 3;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    // NEW: Check if maximum attempts exceeded
    public boolean isMaxAttemptsExceeded() {
        return attemptCount >= MAX_ATTEMPTS;
    }
}
