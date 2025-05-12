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
}
