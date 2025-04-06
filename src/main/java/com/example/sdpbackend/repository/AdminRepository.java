package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Admin;
import com.example.sdpbackend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin,Integer> {
    Admin findByUsername(String username);
}
