package com.example.aihr.aicore.web.dto;

import java.util.Map;

/**
 * 薪酬异常DTO
 */
public class SalaryAnomalyDto {
    private String employeeId;
    private String anomalyType;
    private String description;
    private double currentSalary;
    private double expectedSalary;
    private double differenceAmount;
    private double differencePercentage;
    private String riskLevel;
    private Map<String, Object> evidence;
    
    // Getters and setters
    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    public String getAnomalyType() {
        return anomalyType;
    }
    public void setAnomalyType(String anomalyType) {
        this.anomalyType = anomalyType;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public double getCurrentSalary() {
        return currentSalary;
    }
    public void setCurrentSalary(double currentSalary) {
        this.currentSalary = currentSalary;
    }
    public double getExpectedSalary() {
        return expectedSalary;
    }
    public void setExpectedSalary(double expectedSalary) {
        this.expectedSalary = expectedSalary;
    }
    public double getDifferenceAmount() {
        return differenceAmount;
    }
    public void setDifferenceAmount(double differenceAmount) {
        this.differenceAmount = differenceAmount;
    }
    public double getDifferencePercentage() {
        return differencePercentage;
    }
    public void setDifferencePercentage(double differencePercentage) {
        this.differencePercentage = differencePercentage;
    }
    public String getRiskLevel() {
        return riskLevel;
    }
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    public Map<String, Object> getEvidence() {
        return evidence;
    }
    public void setEvidence(Map<String, Object> evidence) {
        this.evidence = evidence;
    }
}
