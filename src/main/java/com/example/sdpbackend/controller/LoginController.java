package com.example.sdpbackend.controller;


import com.example.sdpbackend.dto.LoginRequest;
import com.example.sdpbackend.dto.LoginResponse;
import com.example.sdpbackend.entity.Admin;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.service.AdminService;
import com.example.sdpbackend.service.CustomerService;
import com.example.sdpbackend.service.JWTService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;



@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class LoginController {
    private final CustomerService customerService;
    private final AdminService adminService;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginController(CustomerService customerService, AdminService adminService, JWTService jwtService, PasswordEncoder passwordEncoder) {
        this.customerService = customerService;
        this.adminService = adminService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isEmpty() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(new LoginResponse("Username and password are required", null));
        }

        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        System.out.println("Username: " + username);  // Log the received username
        System.out.println("Password: " + password);

        System.out.println("Trying to authenticate user: " + username); // Log the username being checked

        // Find customer
        Customer customer = customerService.findByUsername(username);
        if (customer != null) {
            System.out.println("Found customer: " + customer.getUsername()); // Log the customer found
            if (passwordEncoder.matches(password, customer.getPassword())) {
                String token = jwtService.generateToken(customer.getUsername(), "CUSTOMER"); // Generate token here
                LoginResponse response = new LoginResponse("CUSTOMER", token);
                response.setFirstName(customer.getFirstName());
                response.setUserId(customer.getcustomerId());
                return ResponseEntity.ok(response);
            } else {
                System.out.println("Password mismatch for customer: " + username); // Log password mismatch
            }
        }

        // Find admin
        Admin admin = adminService.findByUsername(username);
        if (admin != null) {
            System.out.println("Found admin: " + admin.getUsername()); // Log the admin found
            if (passwordEncoder.matches(password, admin.getPassword())) {
                String token = jwtService.generateToken(admin.getUsername(), "ADMIN"); // Generate token here
                LoginResponse response = new LoginResponse("ADMIN", token);
                response.setFirstName(admin.getFirstName());
                response.setUserId(admin.getAdminId());
                return ResponseEntity.ok(response);
            } else {
                System.out.println("Password mismatch for admin: " + username); // Log password mismatch
            }
        }

        System.out.println("Invalid credentials"); // Log invalid credentials
        return ResponseEntity.badRequest().body(new LoginResponse("Invalid credentials", null));
    }
}




