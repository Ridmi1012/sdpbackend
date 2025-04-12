package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

}
