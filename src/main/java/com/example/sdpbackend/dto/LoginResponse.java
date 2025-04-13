package com.example.sdpbackend.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String userType;
    private String token;
    private String firstName;
    private int userId;

    public LoginResponse(String userType, String token) {
        this.userType = userType;
        this.token = token;
    }
}
