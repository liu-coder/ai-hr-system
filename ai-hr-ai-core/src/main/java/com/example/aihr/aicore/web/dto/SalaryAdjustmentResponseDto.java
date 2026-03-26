package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 薪酬调整响应DTO
 */
public class SalaryAdjustmentResponseDto {
    private List<SalaryAdjustmentDto> adjustments;
    private double totalAdjustmentAmount;
    private double averageAdjustmentPercentage;
    private double budgetImpact;
    
    // Getters and setters
    public List<SalaryAdjustmentDto> getAdjustments() {
        return adjustments;
    }
    public void setAdjustments(List<SalaryAdjustmentDto> adjustments) {
        this.adjustments = adjustments;
    }
    public double getTotalAdjustmentAmount() {
        return totalAdjustmentAmount;
    }
    public void setTotalAdjustmentAmount(double totalAdjustmentAmount) {
        this.totalAdjustmentAmount = totalAdjustmentAmount;
    }
    public double getAverageAdjustmentPercentage() {
        return averageAdjustmentPercentage;
    }
    public void setAverageAdjustmentPercentage(double averageAdjustmentPercentage) {
        this.averageAdjustmentPercentage = averageAdjustmentPercentage;
    }
    public double getBudgetImpact() {
        return budgetImpact;
    }
    public void setBudgetImpact(double budgetImpact) {
        this.budgetImpact = budgetImpact;
    }
}
