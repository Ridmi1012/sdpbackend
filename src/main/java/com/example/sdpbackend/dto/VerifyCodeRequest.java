package com.example.sdpbackend.dto;

import lombok.Data;

@Data
public class VerifyCodeRequest {
    private String username;
    private String code;
}
