package com.example.sdpbackend.entity;

public enum OrderType {
    AS_IS,        // Scenario 1: Order same design with name/age customization
    CUSTOMIZED,   // Scenario 2: Modified existing design (add/remove items, change theme/color)
    FULLY_CUSTOM  // Scenario 3: Completely new design based on theme/concept/inspiration
}
