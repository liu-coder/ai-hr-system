package com.example.aihr.aicore.web.dto;

/**
 * 薪酬调整DTO
 */
public class SalaryAdjustmentDto {
    private String employeeId;
    private double currentSalary;
    private double suggestedSalary;
    private double adjustmentAmount;
    private double adjustmentPercentage;
    private String reason;
    
    // Getters and setters
    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    public double getCurrentSalary() {
        return currentSalary;
    }
    public void setCurrentSalary(double currentSalary) {
        this.currentSalary = currentSalary;
    }
    public double getSuggestedSalary() {
        return suggestedSalary;
    }
    public void setSuggestedSalary(double suggestedSalary) {
        this.suggestedSalary = suggestedSalary;
    }
    public double getAdjustmentAmount() {
        return adjustmentAmount;
    }
    public void setAdjustmentAmount(double adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
    }
    public double getAdjustmentPercentage() {
        return adjustmentPercentage;
    }
    public void setAdjustmentPercentage(double adjustmentPercentage) {
        this.adjustmentPercentage = adjustmentPercentage;
    }
    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
}
