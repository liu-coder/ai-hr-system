package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 薪酬异常检测响应DTO
 */
public class SalaryAnomalyDetectionResponseDto {
    private List<SalaryAnomalyDto> anomalies;
    private int totalAnomalies;
    private int highRiskAnomalies;
    private int mediumRiskAnomalies;
    private int lowRiskAnomalies;
    
    // Getters and setters
    public List<SalaryAnomalyDto> getAnomalies() {
        return anomalies;
    }
    public void setAnomalies(List<SalaryAnomalyDto> anomalies) {
        this.anomalies = anomalies;
    }
    public int getTotalAnomalies() {
        return totalAnomalies;
    }
    public void setTotalAnomalies(int totalAnomalies) {
        this.totalAnomalies = totalAnomalies;
    }
    public int getHighRiskAnomalies() {
        return highRiskAnomalies;
    }
    public void setHighRiskAnomalies(int highRiskAnomalies) {
        this.highRiskAnomalies = highRiskAnomalies;
    }
    public int getMediumRiskAnomalies() {
        return mediumRiskAnomalies;
    }
    public void setMediumRiskAnomalies(int mediumRiskAnomalies) {
        this.mediumRiskAnomalies = mediumRiskAnomalies;
    }
    public int getLowRiskAnomalies() {
        return lowRiskAnomalies;
    }
    public void setLowRiskAnomalies(int lowRiskAnomalies) {
        this.lowRiskAnomalies = lowRiskAnomalies;
    }
}



