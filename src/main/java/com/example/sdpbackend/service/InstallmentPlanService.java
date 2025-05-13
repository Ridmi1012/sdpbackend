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

    @Transactional
    public void initializeDefaultPlans() {
        logger.info("Initializing default installment plans");

        // Create Full Payment plan
        InstallmentPlan fullPayment = new InstallmentPlan();
        fullPayment.setName("Full Payment");
        fullPayment.setNumberOfInstallments(1);
        fullPayment.setPercentages(Arrays.asList(100.0));
        fullPayment.setDescription("Pay the full amount in one payment");
        fullPayment.setIsActive(true);
        installmentPlanRepository.save(fullPayment);

        // Create 50-50 plan
        InstallmentPlan fiftyFifty = new InstallmentPlan();
        fiftyFifty.setName("50% Split");
        fiftyFifty.setNumberOfInstallments(2);
        fiftyFifty.setPercentages(Arrays.asList(50.0, 50.0));
        fiftyFifty.setDescription("Pay 50% now, 50% later");
        fiftyFifty.setIsActive(true);
        installmentPlanRepository.save(fiftyFifty);

        // Create 33-33-34 plan
        InstallmentPlan threeWay = new InstallmentPlan();
        threeWay.setName("33% Split");
        threeWay.setNumberOfInstallments(3);
        threeWay.setPercentages(Arrays.asList(33.34, 33.33, 33.33));
        threeWay.setDescription("Pay in three equal installments");
        threeWay.setIsActive(true);
        installmentPlanRepository.save(threeWay);

        // Create 25% quarterly plan
        InstallmentPlan quarterly = new InstallmentPlan();
        quarterly.setName("25% Split");
        quarterly.setNumberOfInstallments(4);
        quarterly.setPercentages(Arrays.asList(25.0, 25.0, 25.0, 25.0));
        quarterly.setDescription("Pay in four equal installments");
        quarterly.setIsActive(true);
        installmentPlanRepository.save(quarterly);

        logger.info("Default installment plans created successfully");
    }
}
