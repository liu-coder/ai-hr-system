package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 智能排班响应DTO
 */
public class SchedulingResponseDto {
    private List<SchedulingShiftDto> shifts;
    private double employeeSatisfactionScore;
    private double workloadBalanceScore;
    private double businessRequirementSatisfactionScore;
    
    // Getters and setters
    public List<SchedulingShiftDto> getShifts() {
        return shifts;
    }
    public void setShifts(List<SchedulingShiftDto> shifts) {
        this.shifts = shifts;
    }
    public double getEmployeeSatisfactionScore() {
        return employeeSatisfactionScore;
    }
    public void setEmployeeSatisfactionScore(double employeeSatisfactionScore) {
        this.employeeSatisfactionScore = employeeSatisfactionScore;
    }
    public double getWorkloadBalanceScore() {
        return workloadBalanceScore;
    }
    public void setWorkloadBalanceScore(double workloadBalanceScore) {
        this.workloadBalanceScore = workloadBalanceScore;
    }
    public double getBusinessRequirementSatisfactionScore() {
        return businessRequirementSatisfactionScore;
    }
    public void setBusinessRequirementSatisfactionScore(double businessRequirementSatisfactionScore) {
        this.businessRequirementSatisfactionScore = businessRequirementSatisfactionScore;
    }
}



