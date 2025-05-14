package com.example.sdpbackend.service;

import com.example.sdpbackend.entity.Admin;
import com.example.sdpbackend.repository.AdminRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public Admin findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    public Admin updateAdmin(Admin admin) {
        return adminRepository.save(admin);
    }
}
