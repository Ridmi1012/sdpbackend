package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {
    // Fix for the error: Use @Query for complex relationships
    @Query("SELECT ip FROM InstallmentPlan ip LEFT JOIN FETCH ip.percentages WHERE ip.id = :id")
    Optional<InstallmentPlan> findByIdWithPercentages(@Param("id") Long id);

    // Other methods you might need
    List<InstallmentPlan> findByIsActive(Boolean isActive);

    @Query("SELECT ip FROM InstallmentPlan ip LEFT JOIN FETCH ip.percentages WHERE ip.isActive = true")
    List<InstallmentPlan> findAllActiveWithPercentages();
}
