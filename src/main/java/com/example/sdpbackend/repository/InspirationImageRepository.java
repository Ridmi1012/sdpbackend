package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.InspirationImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspirationImageRepository extends JpaRepository<InspirationImage, Long> {
    List<InspirationImage> findByOrderId(Long orderId);
}
