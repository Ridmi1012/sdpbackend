package com.example.sdpbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PasswordChangeRequest {
    // Getters and setters
    private String username;
    private String currentPassword;
    private String newPassword;


}
