package com.example.aihr.aicore.web.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * 异常打卡DTO
 */
public class AttendanceAnomalyDto {
    private String employeeId;
    private LocalDate date;
    private String anomalyType;
    private String description;
    private String riskLevel;
    private Map<String, Object> evidence;
    
    // Getters and setters
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
