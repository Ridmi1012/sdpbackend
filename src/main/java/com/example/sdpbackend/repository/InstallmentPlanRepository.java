package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {
    // Find plan by name
    InstallmentPlan findByName(String name);
}
