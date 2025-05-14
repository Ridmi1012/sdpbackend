package com.example.sdpbackend.dto;


import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}
