package com.example.sdpbackend.dto;

public class PasswordChangeRequest {
    private String username;
    private String currentPassword;
    private String newPassword;

    // Default constructor
    public PasswordChangeRequest() {
    }

    // Constructor with parameters
    public PasswordChangeRequest(String username, String currentPassword, String newPassword) {
        this.username = username;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
