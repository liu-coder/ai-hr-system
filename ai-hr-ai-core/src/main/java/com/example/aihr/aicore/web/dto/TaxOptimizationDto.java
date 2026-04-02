package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 税务优化DTO
 */
public class TaxOptimizationDto {
    private String optimizationType;
    private String description;
    private double estimatedTaxSavings;
    private List<String> implementationSteps;
    
    // Getters and setters
    public String getOptimizationType() {
        return optimizationType;
    }
    public void setOptimizationType(String optimizationType) {
        this.optimizationType = optimizationType;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public double getEstimatedTaxSavings() {
        return estimatedTaxSavings;
    }
    public void setEstimatedTaxSavings(double estimatedTaxSavings) {
        this.estimatedTaxSavings = estimatedTaxSavings;
    }
    public List<String> getImplementationSteps() {
        return implementationSteps;
    }
    public void setImplementationSteps(List<String> implementationSteps) {
        this.implementationSteps = implementationSteps;
    }
}



