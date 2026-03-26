package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 税务优化响应DTO
 */
public class TaxOptimizationResponseDto {
    private String employeeId;
    private double currentTaxableIncome;
    private double currentTax;
    private List<TaxOptimizationDto> optimizations;
    private double totalEstimatedTaxSavings;
    private double projectedTax;
    private double taxReductionPercentage;
    
    // Getters and setters
    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    public double getCurrentTaxableIncome() {
        return currentTaxableIncome;
    }
    public void setCurrentTaxableIncome(double currentTaxableIncome) {
        this.currentTaxableIncome = currentTaxableIncome;
    }
    public double getCurrentTax() {
        return currentTax;
    }
    public void setCurrentTax(double currentTax) {
        this.currentTax = currentTax;
    }
    public List<TaxOptimizationDto> getOptimizations() {
        return optimizations;
    }
    public void setOptimizations(List<TaxOptimizationDto> optimizations) {
        this.optimizations = optimizations;
    }
    public double getTotalEstimatedTaxSavings() {
        return totalEstimatedTaxSavings;
    }
    public void setTotalEstimatedTaxSavings(double totalEstimatedTaxSavings) {
        this.totalEstimatedTaxSavings = totalEstimatedTaxSavings;
    }
    public double getProjectedTax() {
        return projectedTax;
    }
    public void setProjectedTax(double projectedTax) {
        this.projectedTax = projectedTax;
    }
    public double getTaxReductionPercentage() {
        return taxReductionPercentage;
    }
    public void setTaxReductionPercentage(double taxReductionPercentage) {
        this.taxReductionPercentage = taxReductionPercentage;
    }
}
