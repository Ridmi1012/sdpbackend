package com.example.sdpbackend.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String userType;
    private String token;

    public LoginResponse(String userType, String token) {
        this.userType = userType;
        this.token = token;
    }
}
