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
     * EXISTING METHOD - ENHANCED with proper plan percentages
     * Initialize default installment plans according to new requirements
     */
    @Transactional
    public void initializeDefaultPlans() {
        logger.info("Initializing default installment plans");

        // Check if plans already exist to avoid duplicates
        long existingPlans = installmentPlanRepository.count();
        if (existingPlans > 0) {
            logger.info("Installment plans already exist, skipping initialization");
            return;
        }

        // Create Full Payment plan (ID will be 1)
        InstallmentPlan fullPayment = new InstallmentPlan();
        fullPayment.setName("Full Payment");
        fullPayment.setNumberOfInstallments(1);
        fullPayment.setPercentages(Arrays.asList(100.0));
        fullPayment.setDescription("Pay the full amount in one payment - available for all orders");
        fullPayment.setIsActive(true);
        installmentPlanRepository.save(fullPayment);

        // Create 50% Split plan (ID will be 2) - Available for 2+ weeks
        InstallmentPlan fiftyFifty = new InstallmentPlan();
        fiftyFifty.setName("50% Split (2 Installments)");
        fiftyFifty.setNumberOfInstallments(2);
        fiftyFifty.setPercentages(Arrays.asList(50.0, 50.0));
        fiftyFifty.setDescription("Pay 50% now, 50% later - available for events 2+ weeks away");
        fiftyFifty.setIsActive(true);
        installmentPlanRepository.save(fiftyFifty);

        // Create 33.3% Split plan (ID will be 3) - Available for 3+ weeks
        InstallmentPlan threeWay = new InstallmentPlan();
        threeWay.setName("33.3% Split (3 Installments)");
        threeWay.setNumberOfInstallments(3);
        threeWay.setPercentages(Arrays.asList(33.34, 33.33, 33.33));
        threeWay.setDescription("Pay in three equal installments - available for events 3+ weeks away");
        threeWay.setIsActive(true);
        installmentPlanRepository.save(threeWay);

        // Create 25% Split plan (ID will be 4) - Available for 4+ weeks
        InstallmentPlan quarterly = new InstallmentPlan();
        quarterly.setName("25% Split (4 Installments)");
        quarterly.setNumberOfInstallments(4);
        quarterly.setPercentages(Arrays.asList(25.0, 25.0, 25.0, 25.0));
        quarterly.setDescription("Pay in four equal installments - available for events 4+ weeks away");
        quarterly.setIsActive(true);
        installmentPlanRepository.save(quarterly);

        logger.info("Default installment plans created successfully:");
        logger.info("1. Full Payment (100%) - All orders");
        logger.info("2. 50% Split (50%, 50%) - Events 2+ weeks away");
        logger.info("3. 33.3% Split (33.34%, 33.33%, 33.33%) - Events 3+ weeks away");
        logger.info("4. 25% Split (25%, 25%, 25%, 25%) - Events 4+ weeks away");
    }

    /**
     * NEW METHOD - Get installment plan by ID with validation
     */
    public InstallmentPlan getInstallmentPlanById(Long planId) {
        return installmentPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Installment plan not found with ID: " + planId));
    }

    /**
     * NEW METHOD - Get all active installment plans
     */
    public List<InstallmentPlan> getAllActivePlans() {
        return installmentPlanRepository.findByIsActive(true);
    }

    /**
     * NEW METHOD - Validate installment plan selection based on time remaining
     */
    public boolean isInstallmentPlanValidForTimeRemaining(Long planId, long weeksUntilEvent) {
        InstallmentPlan plan = getInstallmentPlanById(planId);

        switch (plan.getNumberOfInstallments()) {
            case 1: // Full payment - always valid
                return true;
            case 2: // 50% split - valid for 2+ weeks
                return weeksUntilEvent >= 2;
            case 3: // 33.3% split - valid for 3+ weeks
                return weeksUntilEvent >= 3;
            case 4: // 25% split - valid for 4+ weeks
                return weeksUntilEvent >= 4;
            default:
                logger.warn("Unknown installment plan with {} installments", plan.getNumberOfInstallments());
                return false;
        }
    }

    /**
     * NEW METHOD - Calculate installment amount for a specific installment number
     */
    public Double calculateInstallmentAmount(Long planId, int installmentNumber, Double totalAmount) {
        InstallmentPlan plan = getInstallmentPlanById(planId);

        if (installmentNumber < 1 || installmentNumber > plan.getNumberOfInstallments()) {
            throw new RuntimeException("Invalid installment number: " + installmentNumber +
                    " for plan with " + plan.getNumberOfInstallments() + " installments");
        }

        Double percentage = plan.getPercentages().get(installmentNumber - 1);
        return (percentage / 100.0) * totalAmount;
    }

    /**
     * NEW METHOD - Get installment plan summary for frontend
     */
    public InstallmentPlanDTO getInstallmentPlanSummary(Long planId) {
        InstallmentPlan plan = getInstallmentPlanById(planId);

        InstallmentPlanDTO dto = new InstallmentPlanDTO();
        dto.setId(plan.getId().intValue());
        dto.setName(plan.getName());
        dto.setNumberOfInstallments(plan.getNumberOfInstallments());
        dto.setPercentages(plan.getPercentages());
        dto.setDescription(plan.getDescription());

        return dto;
    }

    /**
     * NEW METHOD - Create custom installment plan (for future use)
     */
    @Transactional
    public InstallmentPlan createCustomPlan(String name, List<Double> percentages, String description) {
        // Validate that percentages add up to 100
        double total = percentages.stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - 100.0) > 0.01) {
            throw new RuntimeException("Installment percentages must add up to 100%, but total is: " + total);
        }

        InstallmentPlan plan = new InstallmentPlan();
        plan.setName(name);
        plan.setNumberOfInstallments(percentages.size());
        plan.setPercentages(percentages);
        plan.setDescription(description);
        plan.setIsActive(true);

        return installmentPlanRepository.save(plan);
    }

    /**
     * NEW METHOD - Deactivate installment plan
     */
    @Transactional
    public void deactivateInstallmentPlan(Long planId) {
        InstallmentPlan plan = getInstallmentPlanById(planId);
        plan.setIsActive(false);
        installmentPlanRepository.save(plan);
        logger.info("Deactivated installment plan: {}", plan.getName());
    }

    /**
     * NEW METHOD - Reactivate installment plan
     */
    @Transactional
    public void reactivateInstallmentPlan(Long planId) {
        InstallmentPlan plan = getInstallmentPlanById(planId);
        plan.setIsActive(true);
        installmentPlanRepository.save(plan);
        logger.info("Reactivated installment plan: {}", plan.getName());
    }
}
