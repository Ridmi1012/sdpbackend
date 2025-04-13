package com.example.sdpbackend.service;

import com.example.sdpbackend.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class CustomerServiceImpl extends CustomerService {

    public CustomerServiceImpl(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        super(customerRepository, passwordEncoder);
    }
}
