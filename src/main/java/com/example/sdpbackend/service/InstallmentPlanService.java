package com.example.sdpbackend.service;

import com.example.sdpbackend.dto.InstallmentPlanDTO;
import com.example.sdpbackend.entity.InstallmentPlan;
import com.example.sdpbackend.repository.InstallmentPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstallmentPlanService {
    private static final Logger logger = LoggerFactory.getLogger(InstallmentPlanService.class);

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    /**
     * Initialize default installment plans if they don't exist
     */
    @Transactional
    public void initializeDefaultPlans() {
        // Check if plans already exist
        if (installmentPlanRepository.count() > 0) {
            logger.info("Installment plans already initialized");
            return;
        }

        logger.info("Initializing default installment plans");

        // Create default plans
        List<InstallmentPlan> defaultPlans = new ArrayList<>();

        // Full Payment (100%)
        InstallmentPlan fullPayment = new InstallmentPlan();
        fullPayment.setName("Full Payment");
        fullPayment.setNumberOfInstallments(1);
        fullPayment.setPercentages(List.of(100.0));
        fullPayment.setDescription("Pay the full amount in one payment");
        fullPayment.setIsActive(true);
        defaultPlans.add(fullPayment);

        // 2 Installments (50% each)
        InstallmentPlan twoInstallments = new InstallmentPlan();
        twoInstallments.setName("50% Split");
        twoInstallments.setNumberOfInstallments(2);
        twoInstallments.setPercentages(Arrays.asList(50.0, 50.0));
        twoInstallments.setDescription("Pay 50% now, 50% later");
        twoInstallments.setIsActive(true);
        defaultPlans.add(twoInstallments);

        // 3 Installments (33.33% each)
        InstallmentPlan threeInstallments = new InstallmentPlan();
        threeInstallments.setName("33% Split");
        threeInstallments.setNumberOfInstallments(3);
        threeInstallments.setPercentages(Arrays.asList(33.34, 33.33, 33.33));
        threeInstallments.setDescription("Pay in three equal installments");
        threeInstallments.setIsActive(true);
        defaultPlans.add(threeInstallments);

        // 4 Installments (25% each)
        InstallmentPlan fourInstallments = new InstallmentPlan();
        fourInstallments.setName("25% Split");
        fourInstallments.setNumberOfInstallments(4);
        fourInstallments.setPercentages(Arrays.asList(25.0, 25.0, 25.0, 25.0));
        fourInstallments.setDescription("Pay in four equal installments");
        fourInstallments.setIsActive(true);
        defaultPlans.add(fourInstallments);

        // Save all plans
        installmentPlanRepository.saveAll(defaultPlans);
        logger.info("Initialized {} default installment plans", defaultPlans.size());
    }

    /**
     * Get available installment plans based on event date
     * Only full payment is available if event is within 10 days
     */
    public List<InstallmentPlanDTO> getAvailableInstallmentPlans(String eventDateStr, Double totalPrice) {
        try {
            // Parse event date
            LocalDate eventDate = LocalDate.parse(eventDateStr);
            LocalDate today = LocalDate.now();

            // Calculate days until event
            long daysUntilEvent = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate);
            logger.info("Days until event: {}", daysUntilEvent);

            // If event is within 10 days, only allow full payment
            List<InstallmentPlan> availablePlans;
            if (daysUntilEvent <= 10) {
                // Only return full payment option
                availablePlans = installmentPlanRepository.findAll().stream()
                        .filter(plan -> plan.getNumberOfInstallments() == 1 && plan.getIsActive())
                        .collect(Collectors.toList());
                logger.info("Event is within 10 days - returning only full payment option");
            } else {
                // Return all active installment plans
                availablePlans = installmentPlanRepository.findAll().stream()
                        .filter(InstallmentPlan::getIsActive)
                        .collect(Collectors.toList());
                logger.info("Event is more than 10 days away - returning all installment plans");
            }

            // Convert to DTOs
            return availablePlans.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting available installment plans: {}", e.getMessage(), e);
            // In case of error, return only full payment
            List<InstallmentPlan> fullPaymentPlan = installmentPlanRepository.findAll().stream()
                    .filter(plan -> plan.getNumberOfInstallments() == 1 && plan.getIsActive())
                    .collect(Collectors.toList());

            return fullPaymentPlan.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Calculate the amount for a specific installment
     */
    public Double calculateInstallmentAmount(Double totalPrice, Long planId, Integer installmentNumber) {
        InstallmentPlan plan = installmentPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Installment plan not found: " + planId));

        if (installmentNumber < 1 || installmentNumber > plan.getNumberOfInstallments()) {
            throw new IllegalArgumentException("Invalid installment number: " + installmentNumber);
        }

        // Get percentage for this installment (index is 0-based)
        Double percentage = plan.getPercentages().get(installmentNumber - 1);

        // Calculate amount
        return (percentage / 100.0) * totalPrice;
    }

    /**
     * Convert entity to DTO
     */
    public InstallmentPlanDTO convertToDTO(InstallmentPlan plan) {
        InstallmentPlanDTO dto = new InstallmentPlanDTO();
        dto.setId(plan.getId().intValue());
        dto.setName(plan.getName());
        dto.setNumberOfInstallments(plan.getNumberOfInstallments());
        dto.setPercentages(plan.getPercentages());
        dto.setDescription(plan.getDescription());
        return dto;
    }

    /**
     * Get an installment plan by ID
     */
    public InstallmentPlan getPlanById(Long id) {
        return installmentPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Installment plan not found with id: " + id));
    }
}
