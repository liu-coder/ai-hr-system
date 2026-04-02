package com.example.aihr.aicore.web.dto;

import java.time.LocalDate;

public class WorkHourForecastDto {
    private String employeeId;
    private LocalDate date;
    private double predictedWorkHours;
    private double confidence;
    private String workloadLevel;
    private double expectedOvertime;
    private double manpowerShortage;
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public double getPredictedWorkHours() {
        return predictedWorkHours;
    }
    
    public void setPredictedWorkHours(double predictedWorkHours) {
        this.predictedWorkHours = predictedWorkHours;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public String getWorkloadLevel() {
        return workloadLevel;
    }
    
    public void setWorkloadLevel(String workloadLevel) {
        this.workloadLevel = workloadLevel;
    }
    
    public double getExpectedOvertime() {
        return expectedOvertime;
    }
    
    public void setExpectedOvertime(double expectedOvertime) {
        this.expectedOvertime = expectedOvertime;
    }
    
    public double getManpowerShortage() {
        return manpowerShortage;
    }
    
    public void setManpowerShortage(double manpowerShortage) {
        this.manpowerShortage = manpowerShortage;
    }
}



