package com.example.sdpbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlanDTO {
    private Integer id;
    private String name;
    private Integer numberOfInstallments;
    private List<Double> percentages;
    private String description;

    // ENHANCED: Additional fields for better frontend integration
    private Boolean isActive;
    private String timeRequirement; // e.g., "2+ weeks", "3+ weeks", etc.

    // NEW: Calculated fields for frontend display
    private List<InstallmentBreakdownDTO> breakdown;

    // NEW: Order-specific tracking fields
    private Integer currentInstallmentNumber; // Which installment is currently due
    private Integer completedInstallments; // How many installments are completed
    private Double totalPaidAmount; // Total amount paid so far
    private Double remainingAmount; // Remaining amount to be paid
    private String overallStatus; // "pending", "in-progress", "completed"

    // NEW: Individual installment status tracking
    private List<InstallmentStatusDTO> installmentStatuses;

    // NEW: Helper method to set time requirement based on number of installments
    public void setTimeRequirementBasedOnInstallments() {
        switch (this.numberOfInstallments) {
            case 1:
                this.timeRequirement = "Available for all orders";
                break;
            case 2:
                this.timeRequirement = "Available for events 2+ weeks away";
                break;
            case 3:
                this.timeRequirement = "Available for events 3+ weeks away";
                break;
            case 4:
                this.timeRequirement = "Available for events 4+ weeks away";
                break;
            default:
                this.timeRequirement = "Custom plan";
        }
    }

    // NEW: Inner class for installment breakdown
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentBreakdownDTO {
        private Integer installmentNumber;
        private Double percentage;
        private Double amount; // Will be calculated based on order total
        private String description; // e.g., "1st installment", "2nd installment"
        private String status; // "pending", "paid", "overdue"
        private Boolean isClickable; // Can customer pay this installment now?
        private Boolean isCurrent; // Is this the current installment to pay?
    }

    // NEW: Inner class for individual installment status tracking
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentStatusDTO {
        private Integer installmentNumber;
        private Double amount;
        private Double percentage;
        private String status; // "pending", "confirmed", "rejected"
        private String paymentMethod; // "payhere", "bank-transfer"
        private String transactionId;
        private String submittedDate;
        private String confirmedDate;
        private String verifiedBy;
        private String rejectionReason;
        private Boolean isClickable; // Can be clicked to make payment
        private Boolean isCurrent; // Current installment to pay
        private Boolean isCompleted; // Is this installment completed
    }

    // NEW: Method to generate breakdown for a specific order total
    public void generateBreakdown(Double orderTotal) {
        if (this.percentages != null && orderTotal != null) {
            this.breakdown = new java.util.ArrayList<>();
            for (int i = 0; i < this.percentages.size(); i++) {
                InstallmentBreakdownDTO breakdownItem = new InstallmentBreakdownDTO();
                breakdownItem.setInstallmentNumber(i + 1);
                breakdownItem.setPercentage(this.percentages.get(i));
                breakdownItem.setAmount((this.percentages.get(i) / 100.0) * orderTotal);

                // Generate description
                String suffix = getSuffix(i + 1);
                breakdownItem.setDescription(String.format("%d%s installment (%.1f%%)",
                        i + 1, suffix, this.percentages.get(i)));

                // Set default status - will be updated based on actual payment status
                breakdownItem.setStatus("pending");
                breakdownItem.setIsClickable(i == 0); // Only first installment clickable initially
                breakdownItem.setIsCurrent(i == 0); // First installment is current initially

                this.breakdown.add(breakdownItem);
            }
        }
    }

    // NEW: Method to update breakdown with actual payment status
    public void updateBreakdownWithPaymentStatus(Integer currentInstallment, List<InstallmentStatusDTO> statuses) {
        if (this.breakdown != null && statuses != null) {
            for (InstallmentBreakdownDTO item : this.breakdown) {
                // Find corresponding status
                InstallmentStatusDTO status = statuses.stream()
                        .filter(s -> s.getInstallmentNumber().equals(item.getInstallmentNumber()))
                        .findFirst()
                        .orElse(null);

                if (status != null) {
                    item.setStatus(status.getStatus());
                    item.setIsClickable(status.getIsClickable());
                    item.setIsCurrent(status.getIsCurrent());
                } else {
                    // Default logic based on current installment
                    if (item.getInstallmentNumber() < currentInstallment) {
                        item.setStatus("paid");
                        item.setIsClickable(false);
                        item.setIsCurrent(false);
                    } else if (item.getInstallmentNumber().equals(currentInstallment)) {
                        item.setStatus("pending");
                        item.setIsClickable(true);
                        item.setIsCurrent(true);
                    } else {
                        item.setStatus("pending");
                        item.setIsClickable(false);
                        item.setIsCurrent(false);
                    }
                }
            }
        }
    }

    // NEW: Calculate overall progress percentage
    public Double getProgressPercentage() {
        if (this.numberOfInstallments == null || this.completedInstallments == null) {
            return 0.0;
        }
        return (this.completedInstallments.doubleValue() / this.numberOfInstallments.doubleValue()) * 100.0;
    }

    // NEW: Check if installment plan is completed
    public Boolean isCompleted() {
        return this.completedInstallments != null &&
                this.numberOfInstallments != null &&
                this.completedInstallments.equals(this.numberOfInstallments);
    }

    // NEW: Get next installment due
    public InstallmentBreakdownDTO getNextInstallmentDue() {
        if (this.breakdown != null) {
            return this.breakdown.stream()
                    .filter(item -> "pending".equals(item.getStatus()) && item.getIsCurrent())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    // Helper method to get proper suffix for numbers
    private String getSuffix(int number) {
        if (number >= 11 && number <= 13) {
            return "th";
        }
        switch (number % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
}
