package com.example.sdpbackend.dto;

import lombok.Data;

@Data
public class CodeResetPasswordRequest {
    private String username;
    private String code;
    private String newPassword;
}
