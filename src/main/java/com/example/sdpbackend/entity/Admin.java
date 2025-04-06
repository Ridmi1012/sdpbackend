package com.example.sdpbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "admin")

public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int adminId;

    @Column(name = "First_Name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "Last_Name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password; // Hashed using BCrypt

    @Column(name = "TelNo", length = 15)
    private String telNo;

    @Column(columnDefinition = "TEXT")
    private String description;
}
